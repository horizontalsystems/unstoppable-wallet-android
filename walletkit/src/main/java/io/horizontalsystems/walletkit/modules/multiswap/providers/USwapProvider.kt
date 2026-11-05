package io.horizontalsystems.walletkit.modules.multiswap.providers

import android.os.Build
import android.util.Base64
import android.util.Log
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.hexStringToByteArray
import io.horizontalsystems.walletkit.core.isEvm
import io.horizontalsystems.walletkit.core.managers.APIClient
import io.horizontalsystems.walletkit.core.stripHexPrefix
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.multiswap.SplitRateException
import io.horizontalsystems.walletkit.modules.multiswap.SplitSwapOutput
import io.horizontalsystems.walletkit.modules.multiswap.SplitSwapQuoteExtraData
import io.horizontalsystems.walletkit.modules.multiswap.SplitSwapRecipient
import io.horizontalsystems.walletkit.modules.multiswap.SplitSwapRules
import io.horizontalsystems.walletkit.modules.multiswap.SwapCommitFailedException
import io.horizontalsystems.walletkit.modules.multiswap.SwapCommitFailure
import io.horizontalsystems.walletkit.modules.multiswap.SwapCommitment
import io.horizontalsystems.walletkit.modules.multiswap.SwapFinalQuote
import io.horizontalsystems.walletkit.modules.multiswap.SwapMevProtection
import io.horizontalsystems.walletkit.modules.multiswap.SwapQuote
import io.horizontalsystems.walletkit.modules.multiswap.ThorChainSwapMemo
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.EvmTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldRecipient
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldSlippage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Collections
import java.util.UUID

/**
 * The one swap provider. One unnamed `/v3/rate` per round answers with every venue's route; each
 * becomes a quote whose venue is the route's provider, handed back here for the preview
 * (`/v3/preview` with that `provider`), the AML precheck and the commit. Venue identity comes
 * from [SwapProviderInfoManager], token identifiers from [USwapAssetCatalog] and Zcash addresses
 * from the route's `addressRules`, so no provider is named anywhere in the app: the server adds
 * or removes one without a release.
 */
class USwapProvider(
    private val providerInfoManager: SwapProviderInfoManager,
    private val assetCatalog: USwapAssetCatalog,
    // Whether /v3/preview gets `sourceAddress` for this sell token: the server-built-tx signal.
    // An app that builds the source leg itself (e.g. a deposit transfer batched into its own
    // signer flow) turns it off so the server returns the plain deposit route instead of
    // building and gas-estimating a transaction.
    private val shouldIncludeSourceAddress: (Token) -> Boolean = { it.needsServerBuiltTx },
    // Builds the EVM source leg of a transfer route: the deposit calldata the app's own
    // signer executes. Defaults to the EVM chain plugin's plain transfer; an app whose
    // signer flow needs another shape supplies its own.
    private val buildEvmDepositTransfer: ((tokenIn: Token, amountIn: BigDecimal, depositAddress: String) -> SendTransactionData.Evm)? = null,
) : ISplitSwapProvider {
    override val id = ID

    // The `X-Session-Id` of the swap screen that is open: generated when it opens and replaced
    // after a swap, so support can line up the quotes the user compared with the swap they made.
    @Volatile
    private var sessionId: String? = null

    private val unstoppableAPI = APIClient.build(App.appConfigProvider.uswapApiBaseUrl) {
        buildMap {
            put("x-api-key", App.appConfigProvider.uswapApiKey)
            put("User-Agent", userAgent)
            sessionId?.let { put("X-Session-Id", it) }
        }
    }.create(UnstoppableAPI::class.java)

    // A venue the directory does not know forces a sync off the quoting path.
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun startSession() {
        sessionId = UUID.randomUUID().toString()
    }

    override suspend fun start() {
        providerInfoManager.sync()
        assetCatalog.sync()
    }

    override fun supports(tokenFrom: Token, tokenTo: Token): Boolean = assetCatalog.canQuote(tokenFrom, tokenTo)

    override fun zcashTransparentRecipientOnly(quote: SwapQuote): Boolean {
        if (quote.tokenOut.blockchainType != BlockchainType.Zcash) return false
        val rules = (quote.extraData as? SwapQuoteExtraData)?.route?.addressRules
        return USwapZcashAddressPolicy.transparentRecipientOnly(rules, quote.tokenIn.blockchainType, executionType(quote.venue))
    }

    override suspend fun fetchQuotes(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
    ): List<SwapQuote> {
        val request = UnstoppableAPI.Request.Rate(
            sellAsset = assetIdentifier(tokenIn),
            buyAsset = assetIdentifier(tokenOut),
            sellAmount = amountIn.toPlainString(),
            slippage = IMultiSwapProvider.DEFAULT_SLIPPAGE,
            chainId = rateChainId(tokenIn),
        )
        val response = unstoppableAPI.rate(request)
        val routes = USwapRouteFilter.filter(response.routes.orEmpty(), tokenIn, providerInfoManager::executionType)
        if (routes.isEmpty()) throw NoSuchElementException("No routes")

        // Built in parallel (the allowance read reaches the chain); the server's order is kept,
        // and a route that cannot be read into a quote is dropped rather than failing the round.
        val quotes = coroutineScope {
            routes.map { route ->
                async {
                    try {
                        quote(route, tokenIn, tokenOut, amountIn)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        Log.e(TAG, "Quote build failed for ${route.providerId}", e)
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
        if (quotes.isEmpty()) throw NoSuchElementException("No routes")

        return quotes
    }

    private suspend fun quote(
        route: UnstoppableAPI.Response.Route,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
    ): SwapQuote {
        val approvalAddress = route.approvalSpenderOrExecution
        val actionRequired = approvalAddress?.let {
            val plugin = ChainRegistry[tokenIn.blockchainType]
            val allowance = plugin?.eip20Allowance(tokenIn, it)
            plugin?.eip20ApproveAction(allowance, amountIn, it, tokenIn)
        } ?: ChainRegistry[tokenOut.blockchainType]?.swapActivationAction(tokenOut)

        return SwapQuote(
            amountOut = route.expectedBuyAmountOrZero,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            actionRequired = actionRequired,
            estimationTime = route.estimatedTime?.total,
            venue = resolveVenue(route.providerId.orEmpty(), route.amlPolicy),
            amlProviderId = route.amlProviderId?.takeIf { it.isNotEmpty() },
            extraData = SwapQuoteExtraData(route),
        )
    }

    // The venue a route is served by. An id the directory does not know yet is still quoted,
    // under a humanized name, and the directory is asked to refresh for it.
    private fun resolveVenue(providerId: String, amlPolicy: String?): SwapVenue {
        val venue = providerInfoManager.venue(providerId)
        if (!venue.isKnown) {
            syncScope.launch {
                try {
                    providerInfoManager.requestSync(providerId)
                } catch (e: Throwable) {
                    Log.e(TAG, "Provider sync for $providerId failed", e)
                }
            }
        }
        return venue.overriding(amlPolicy)
    }

    // The directory's current word on how the venue settles, falling back to what the quote
    // captured when the directory has not listed it yet.
    private fun executionType(venue: SwapVenue): String? =
        providerInfoManager.executionType(venue.id) ?: venue.executionType

    override suspend fun checkAmlAddresses(addresses: List<String>, amlProviderId: String): Boolean? =
        unstoppableAPI.checkAddresses(addresses.joinToString(","), amlProviderId).passedAmlCheck

    // ----- Exact output: private send (same asset, confidential) and CrossPay (cross asset) -----

    /**
     * `/v3/rate` in exact-output mode, unnamed: `buyAmount` is what the recipient must receive,
     * and every provider that can price backwards answers. [privacy] narrows the fan-out to those
     * privacy levels; null includes every level. Returns the whole response so the caller can
     * read `providerErrors` when no route comes back.
     */
    suspend fun exactOutputRate(
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigDecimal,
        slippage: BigDecimal,
        privacy: List<String>? = null,
    ): UnstoppableAPI.Response.Rate = unstoppableAPI.rate(
        UnstoppableAPI.Request.Rate(
            sellAsset = assetIdentifier(tokenIn),
            buyAsset = assetIdentifier(tokenOut),
            slippage = slippage,
            privacy = privacy,
            chainId = rateChainId(tokenIn),
            buyAmount = amountOut.toPlainString(),
        )
    )

    /**
     * `/v3/preview` in exact-output mode with the one [venueId] the rate round chose: no order yet,
     * the route carries the token [commitPreview] turns into one. With [includeSourceAddress] the
     * swap screen's build-signal contract applies verbatim; a private send keeps it off entirely,
     * since handing the provider the sender's address defeats the point. A ZEC sell's refund and
     * source follow the route's [addressRules] instead.
     */
    suspend fun exactOutputPreview(
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigDecimal,
        destinationAddress: String,
        refundAddress: String,
        slippage: BigDecimal,
        venueId: String,
        includeSourceAddress: Boolean = false,
        addressRules: UnstoppableAPI.Response.ZcashAddressRules? = null,
    ): UnstoppableAPI.Response.Route {
        val sourceAddress = when {
            tokenIn.blockchainType == BlockchainType.Zcash -> zcashSourceAddress(tokenIn, addressRules, refundAddress)
            includeSourceAddress && shouldIncludeSourceAddress(tokenIn) -> SwapHelper.getSendingAddressForToken(tokenIn)
            else -> null
        }

        val route = unstoppableAPI.preview(
            UnstoppableAPI.Request.Preview(
                sellAsset = assetIdentifier(tokenIn),
                buyAsset = assetIdentifier(tokenOut),
                slippage = slippage,
                provider = venueId,
                destinationAddress = destinationAddress,
                refundAddress = refundAddress,
                sourceAddress = sourceAddress,
                chainId = rateChainId(tokenIn),
                buyAmount = amountOut.toPlainString(),
            )
        )

        if (route.previewToken.isNullOrEmpty()) {
            throw IllegalStateException("Preview carries no token")
        }

        return route
    }

    // /v3/commit for a route previewed here. Throws [UnstoppableAPI.CommitException] on refusal.
    // The commit's `execution` carries the deposit address, amount and attachment.
    suspend fun commitPreview(previewToken: String): UnstoppableAPI.Response.Commit {
        // A token is spent by its first commit, whatever the outcome: a refused one may be retried
        // server-side and run a second provider order, and an accepted one is replayed, which
        // would fund the same order twice. Either way the screen no longer describes it.
        if (!consumedPreviewTokens.add(previewToken)) {
            throw UnstoppableAPI.CommitException(null, "refresh_required", "Preview already committed", null)
        }

        currentCoroutineContext().ensureActive()

        val commit = try {
            unstoppableAPI.commit(UnstoppableAPI.Request.Commit(previewToken))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw UnstoppableAPI.CommitException.from(e)
        }

        if (commit.status != "ok" || commit.uuid.isNullOrEmpty()) {
            throw UnstoppableAPI.CommitException(null, commit.status, null, null)
        }

        return commit
    }

    /**
     * The deposit transfer for a route committed in exact-output mode: exactly
     * [depositAmount] (the route's execution.amount) to its deposit address, in the same
     * per-chain shapes the swap confirmation uses — including the attachment
     * deliverability rules. Chains that consume a server-built tx must have committed with
     * the source address included, exactly as the swap flow does.
     */
    fun depositTransactionData(
        tokenIn: Token,
        depositAmount: BigDecimal,
        route: UnstoppableAPI.Response.Route,
    ): SendTransactionData = getSendTransactionData(tokenIn, depositAmount, route)

    /**
     * The deposit transfer of exactly [depositAmount] to [depositAddress] carrying [attachment],
     * for a route whose execution is known from its parts: a committed order's deposit, or the
     * preview's stand-in address (with no attachment) the confirmation screen prices while the
     * real one does not exist yet.
     */
    fun depositTransactionData(
        tokenIn: Token,
        depositAmount: BigDecimal,
        depositAddress: String,
        attachment: UnstoppableAPI.Response.Attachment?,
    ): SendTransactionData {
        val route = UnstoppableAPI.Response.Route(
            sellAsset = null,
            buyAsset = null,
            expectedBuyAmount = null,
            minBuyAmount = null,
            estimatedTime = null,
            approvalSpender = null,
            execution = transferExecution(depositAddress, attachment),
            uuid = null,
        )
        return getSendTransactionData(tokenIn, depositAmount, route)
    }

    /**
     * The address a sell of [tokenIn] is refunded to: the wallet's own address on the sell chain,
     * which for ZEC is what the route's [addressRules] recommend, a unified address where the rule
     * names one and the transparent address otherwise. Exact-output flows name it too, so a
     * CrossPay buffer refund follows the same rule as a swap refund.
     */
    suspend fun refundAddress(tokenIn: Token, addressRules: UnstoppableAPI.Response.ZcashAddressRules?): String =
        if (tokenIn.blockchainType == BlockchainType.Zcash) {
            zcashAddress(tokenIn, USwapZcashAddressPolicy.refundKind(addressRules))
        } else {
            SwapHelper.getReceiveAddressForToken(tokenIn)
        }

    // ----- Zcash addresses, from the route's rules -----

    // A ZEC address of the kind a rule names: the transparent address, the wallet's unified
    // address when the rule's receivers include a transparent one, or a unified address with
    // shielded receivers only when they do not.
    private suspend fun zcashAddress(token: Token, kind: ZcashAddressKind): String = when (kind) {
        ZcashAddressKind.Transparent -> SwapHelper.getReceiveAddressForToken(token)
        is ZcashAddressKind.Unified -> {
            val plugin = ChainRegistry[BlockchainType.Zcash] ?: throw IllegalStateException("Zcash support is not available")
            val address = if (kind.carriesTransparent) plugin.swapUnifiedReceiveAddress(token) else plugin.swapShieldedRefundAddress(token)
            address ?: throw IllegalStateException("Zcash support is not available")
        }
    }

    // What a ZEC sell names as `sourceAddress`: nothing without a source rule, the refund address
    // when the provider refunds to the sender, or an address of the rule's own kind.
    private suspend fun zcashSourceAddress(
        token: Token,
        addressRules: UnstoppableAPI.Response.ZcashAddressRules?,
        refundAddress: String,
    ): String? = when (val policy = USwapZcashAddressPolicy.sourcePolicy(addressRules)) {
        ZcashSourcePolicy.Omit -> null
        ZcashSourcePolicy.SameAsRefund -> refundAddress
        is ZcashSourcePolicy.Own -> zcashAddress(token, policy.kind)
    }

    // ----- The confirmation screen -----

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: SwapQuote,
        recipient: Address?,
        slippage: BigDecimal,
    ): SwapFinalQuote {
        val selectedRoute = (swapQuote.extraData as? SwapQuoteExtraData)?.route
            ?: throw IllegalStateException("Quote carries no route")
        val venueId = swapQuote.venue.id

        var request = previewRequest(selectedRoute.addressRules, venueId, tokenIn, tokenOut, amountIn, slippage, swapQuote.venue, recipient, sendTransactionSettings)

        val bestRoute = try {
            previewSwap(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            UnstoppableAPI.AddressBlacklistedException.from(e)?.let { throw it }
            // A Zcash address the route does not take: preview ONCE more with the addresses the
            // server's rules point to; a second refusal shows the server's message.
            val addressError = UnstoppableAPI.PreviewAddressException.from(e) ?: throw e
            val correctedRules = addressError.addressRules ?: throw addressError
            request = previewRequest(correctedRules, venueId, tokenIn, tokenOut, amountIn, slippage, swapQuote.venue, recipient, sendTransactionSettings)
            try {
                previewSwap(request)
            } catch (e2: CancellationException) {
                throw e2
            } catch (e2: Throwable) {
                throw UnstoppableAPI.AddressBlacklistedException.from(e2)
                    ?: UnstoppableAPI.PreviewAddressException.from(e2)
                    ?: e2
            }
        }

        val destination = request.destinationAddress

        // A THORChain-style deposit memo is what routes the outbound leg: a route without one
        // cannot be delivered, and one whose destination is not the recipient shown to the user
        // must not be signed.
        bestRoute.execution?.takeIf { ThorChainDepositRoute.isVaultDeposit(it) }?.let { execution ->
            val memo = execution.memo ?: throw IllegalStateException("No THORChain deposit memo")
            ThorChainSwapMemo.requireDestination(memo, destination, tokenOut.blockchainType)
        }

        val amountOut = bestRoute.expectedBuyAmountOrZero

        // The server's `minBuyAmount` is the enforced floor the route can deliver — show it
        // directly as the "Guaranteed" amount instead of deriving one from slippage. `null`
        // means the route is a floating P2P estimate — nothing guarantees the amount (or applies
        // our slippage), so the confirm page must not show the "Guaranteed" or slippage rows.
        val amountOutMin = bestRoute.minBuyAmount
        val effectiveSlippage = if (bestRoute.minBuyAmount != null) slippage else null

        val fields = buildList {
            recipient?.let {
                add(DataFieldRecipient(it))
            }
            DataFieldSlippage.getField(effectiveSlippage)?.let {
                add(it)
            }
        }

        val execution = bestRoute.execution
        val sendTransactionData = if (execution != null) {
            getSendTransactionData(tokenIn, amountIn, bestRoute)
        } else {
            // A transfer provider's deposit address exists only once the order does, so the
            // confirmation screen estimates the transfer against the server's stand-in account,
            // and [commit] rebuilds it for the real address. The screen shows no deposit address
            // meanwhile: the stand-in must never be mistaken for one. Without a stand-in (some
            // sell chains have none configured) there is nothing to estimate against, and the
            // quote is shown without a network fee instead of being refused.
            bestRoute.stubDepositAddress?.takeIf { it.isNotBlank() }?.let { stub ->
                getSendTransactionData(tokenIn, amountIn, bestRoute.copy(execution = transferExecution(stub, estimationAttachment(tokenIn))))
            }
        }

        return SwapFinalQuote(
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            amountOut = amountOut,
            amountOutMin = amountOutMin,
            sendTransactionData = sendTransactionData,
            priceImpact = null,
            fields = fields,
            estimatedTime = bestRoute.estimatedTime?.total,
            slippage = effectiveSlippage,
            providerSwapId = null,
            fromAsset = request.sellAsset,
            toAsset = request.buyAsset,
            depositAddress = execution?.resolvedDepositAddress(),
            // XRP's identifier is a destination tag, not a memo — the XRP send service shows it
            // under its own label, so showing it here again as a "deposit memo" would misname it.
            depositMemo = execution
                ?.takeIf { tokenIn.blockchainType != BlockchainType.Xrp }
                ?.resolvedMemo(),
            approvalSpender = bestRoute.approvalSpenderOrExecution,
            providerContext = PreviewContext(
                previewToken = bestRoute.previewToken ?: throw IllegalStateException("Preview carries no token"),
                route = bestRoute,
                tokenIn = tokenIn,
                amountIn = amountIn,
            ),
            mevProtectionAllowed = SwapMevProtection.allowedForRoute(execution, tokenIn, tokenOut),
        )
    }

    // The /v3/preview body for the route's provider: the addresses a ZEC side takes come from
    // [addressRules], and `sourceAddress` is the build signal — sent only for chains whose
    // server-built signed_transaction / thorchain_deposit tx we consume (by default EVM/Tron/
    // TON/Solana/Stellar), where it is also required for the tx `from`. For UTXO/Monero/Zano
    // (and any token the host app opts out of) it is omitted; a transfer route's deposit is
    // built locally on every chain either way. A ZEC sell is the exception: its source follows
    // the route's source rule, independent of the build signal.
    private suspend fun previewRequest(
        addressRules: UnstoppableAPI.Response.ZcashAddressRules?,
        venueId: String,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        slippage: BigDecimal,
        venue: SwapVenue,
        recipient: Address?,
        sendTransactionSettings: SendTransactionSettings?,
    ): UnstoppableAPI.Request.Preview {
        val destination = when {
            recipient != null -> recipient.hex
            tokenOut.blockchainType == BlockchainType.Zcash -> zcashAddress(
                tokenOut,
                USwapZcashAddressPolicy.destinationKind(addressRules, tokenIn.blockchainType, executionType(venue)),
            )
            else -> SwapHelper.getReceiveAddressForToken(tokenOut)
        }

        val refundAddress = refundAddress(tokenIn, addressRules)
        val sourceAddress = when {
            tokenIn.blockchainType == BlockchainType.Zcash -> zcashSourceAddress(tokenIn, addressRules, refundAddress)
            shouldIncludeSourceAddress(tokenIn) -> SwapHelper.getSendingAddressForToken(tokenIn)
            else -> null
        }

        return UnstoppableAPI.Request.Preview(
            sellAsset = assetIdentifier(tokenIn),
            buyAsset = assetIdentifier(tokenOut),
            sellAmount = amountIn.toPlainString(),
            slippage = slippage,
            provider = venueId,
            destinationAddress = destination,
            refundAddress = refundAddress,
            sourceAddress = sourceAddress,
            chainId = rateChainId(tokenIn),
            networkFee = UnstoppableAPI.Request.NetworkFee.of(tokenIn, sendTransactionSettings),
        )
    }

    // /v3/preview — the committed route shape for one provider, without creating an order. The
    // route must carry the token that [commit] later turns into an order.
    private suspend fun previewSwap(request: UnstoppableAPI.Request.Preview): UnstoppableAPI.Response.Route {
        val route = unstoppableAPI.preview(request)

        if (route.previewToken.isNullOrEmpty()) {
            throw IllegalStateException("Preview carries no token")
        }

        return route
    }

    // A plain-transfer execution known from its parts, so the per-chain builders serve a deposit
    // whether it came in a route or is being priced against the preview's stand-in address.
    private fun transferExecution(depositAddress: String, attachment: UnstoppableAPI.Response.Attachment?) =
        UnstoppableAPI.Response.Execution(
            method = "transfer",
            chain = null,
            transactions = null,
            approval = null,
            depositAddress = depositAddress,
            amount = null,
            asset = null,
            attachment = attachment,
            unsignedTx = null,
            protocol = null,
            inboundAddress = null,
            memo = null,
            delivery = null,
        )

    // The attachment a stand-in estimate carries: Stellar payments to a provider name a memo, so a
    // placeholder stands in for its size and for the builder's requirement that one is present.
    private fun estimationAttachment(tokenIn: Token): UnstoppableAPI.Response.Attachment? =
        if (tokenIn.blockchainType == BlockchainType.Stellar) UnstoppableAPI.Response.Attachment("text", "0") else null

    override suspend fun commit(finalQuote: SwapFinalQuote): SwapCommitment {
        // A split preview is committed through its own endpoint; its token is refused by /v3/commit.
        (finalQuote.providerContext as? SplitPreviewContext)?.let { return commitSplit(finalQuote, it) }

        // Every quote this provider returns carries a context; a missing one can only mean the
        // quote came from elsewhere, and executing it as-is would skip creating the order.
        val context = finalQuote.providerContext as? PreviewContext
            ?: throw SwapCommitFailedException(SwapCommitFailure.Failed, IllegalStateException("Quote was not previewed"))

        // A consumed token can never create a second order (the server replays the first), but
        // committing it again would hand back a deposit the screen no longer describes.
        if (!consumedPreviewTokens.add(context.previewToken)) {
            throw SwapCommitFailedException(SwapCommitFailure.RefreshRequired)
        }

        // commit has side effects: never create an order from a cancelled task
        currentCoroutineContext().ensureActive()

        val commit = try {
            unstoppableAPI.commit(UnstoppableAPI.Request.Commit(context.previewToken))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val commitException = UnstoppableAPI.CommitException.from(e)
            throw SwapCommitFailedException(commitException.toSwapCommitFailure(), commitException)
        }

        val uuid = commit.uuid
        if (commit.status != "ok" || uuid.isNullOrEmpty()) {
            throw SwapCommitFailedException(SwapCommitFailure.Failed, IllegalStateException("Commit answered ${commit.status} without a uuid"))
        }

        val execution = commit.execution
            ?: run {
                // Nothing to rebuild: only a preview that carried its own execution can be sent as
                // previewed. A transfer preview was built against the stand-in and has nowhere to go.
                if (context.route.execution == null) {
                    throw SwapCommitFailedException(SwapCommitFailure.Failed, IllegalStateException("Commit returned no deposit"))
                }
                return SwapCommitment(finalQuote.copy(providerSwapId = uuid, providerContext = null), uuid)
            }

        try {
            // A signed / thorchain preview already carries its final transaction; a different one
            // at commit would mean signing something the user never reviewed. Only a plain transfer
            // is ever rebuilt here, from its deposit address and attachment.
            val previewedExecution = context.route.execution
            if (previewedExecution != null && previewedExecution.method != "transfer") {
                throw IllegalStateException("Commit replaced a ${previewedExecution.method} execution")
            }
            if (execution.method != "transfer") {
                throw IllegalStateException("Commit returned a ${execution.method} execution")
            }
            // The order's floor can only hold or improve on what the user confirmed.
            val previewedMin = context.route.minBuyAmount
            val committedMin = commit.minBuyAmount
            if (previewedMin != null && committedMin != null && committedMin < previewedMin) {
                throw IllegalStateException("Commit lowered the guaranteed amount")
            }

            // The transfer moves the amount the user saw; the commit's sellAmount is not consulted.
            val route = context.route.merging(commit)
            val quote = finalQuote.copy(
                amountOut = route.expectedBuyAmountOrZero,
                amountOutMin = route.minBuyAmount,
                sendTransactionData = getSendTransactionData(context.tokenIn, context.amountIn, route),
                providerSwapId = uuid,
                depositAddress = execution.resolvedDepositAddress(),
                depositMemo = execution
                    .takeIf { context.tokenIn.blockchainType != BlockchainType.Xrp }
                    ?.resolvedMemo(),
                providerContext = null,
            )
            return SwapCommitment(quote, uuid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SwapCommitFailedException) {
            throw e
        } catch (e: Throwable) {
            // The order exists server-side and lapses unfunded; the user gets a fresh quote.
            throw SwapCommitFailedException(SwapCommitFailure.Failed, e)
        }
    }

    override suspend fun reportInboundTxHash(providerSwapId: String, txHash: String) {
        unstoppableAPI.signed(UnstoppableAPI.Request.Signed(providerSwapId, txHash))
    }

    // ----- Split swaps: one deposit, several outputs of the same buy token -----

    override suspend fun fetchSplitQuotes(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        percents: List<Int>,
    ): List<SwapQuote> {
        SplitSwapRules.requireValid(percents)

        val assetOut = assetIdentifier(tokenOut)
        val request = UnstoppableAPI.Request.SplitRate(
            sellAsset = assetIdentifier(tokenIn),
            sellAmount = amountIn.toPlainString(),
            slippage = IMultiSwapProvider.DEFAULT_SLIPPAGE,
            outputs = percents.map { UnstoppableAPI.Request.SplitOutput(buyAsset = assetOut, amount = it.toString()) },
            chainId = rateChainId(tokenIn),
        )

        // /v3/split/rate, unnamed: only split-capable providers answer. A refusal is a non-2xx
        // carrying providerErrors (a share under the per-output minimum, a pair no one quotes).
        val response = try {
            unstoppableAPI.splitRate(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            throw SplitRateException.from(e)
        }

        // A route must price every requested output, or an output would have no amount to show,
        // and its venue must be one the directory flags as serving split swaps.
        val quotes = response.routes.orEmpty()
            .filter { route -> route.outputsByIndex.map { it.index } == percents.indices.toList() }
            .filter { route -> route.providerId?.let { providerInfoManager.supportsSplitSwaps(it) } == true }
            .map { route ->
                SwapQuote(
                    amountOut = route.expectedBuyAmountTotal,
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    amountIn = amountIn,
                    // The deposit is a plain transfer, so the sell side never needs an approval; the buy
                    // side may still need the account to accept the token (a Stellar trustline).
                    actionRequired = ChainRegistry[tokenOut.blockchainType]?.swapActivationAction(tokenOut),
                    estimationTime = route.estimatedTime?.total,
                    venue = resolveVenue(route.providerId.orEmpty(), route.amlPolicy),
                    extraData = SplitSwapQuoteExtraData(route, percents),
                )
            }

        if (quotes.isEmpty()) throw SplitRateException(response.providerErrors.orEmpty(), "No split routes")

        return quotes
    }

    override suspend fun fetchSplitFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        recipients: List<SplitSwapRecipient>,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: SwapQuote,
        slippage: BigDecimal,
    ): SwapFinalQuote {
        SplitSwapRules.requireValid(recipients.map { it.percent })

        // The same decision as for a single swap: where a failed deposit is refunded, and whether
        // the sender is named. A split is always a transfer the app builds, so the source
        // address is only recorded as the sender. Split routes carry no `addressRules` (no split
        // provider lists ZEC), so a ZEC refund is the transparent address and no source is named.
        val refundAddress = refundAddress(tokenIn, null)
        val sourceAddress = when {
            tokenIn.blockchainType == BlockchainType.Zcash -> zcashSourceAddress(tokenIn, null, refundAddress)
            shouldIncludeSourceAddress(tokenIn) -> SwapHelper.getSendingAddressForToken(tokenIn)
            else -> null
        }

        val assetIn = assetIdentifier(tokenIn)
        val assetOut = assetIdentifier(tokenOut)
        val request = UnstoppableAPI.Request.SplitPreview(
            sellAsset = assetIn,
            sellAmount = amountIn.toPlainString(),
            slippage = slippage,
            provider = swapQuote.venue.id,
            refundAddress = refundAddress,
            sourceAddress = sourceAddress,
            chainId = rateChainId(tokenIn),
            networkFee = UnstoppableAPI.Request.NetworkFee.of(tokenIn, sendTransactionSettings),
            outputs = recipients.map {
                UnstoppableAPI.Request.SplitOutput(buyAsset = assetOut, amount = it.percent.toString(), destinationAddress = it.address.hex)
            },
        )

        val route = try {
            unstoppableAPI.splitPreview(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            throw SplitRateException.from(e)
        }

        val previewToken = route.previewToken?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Preview carries no token")

        val outputs = previewedOutputs(route, recipients)

        // A split preview never names the deposit: the transfer is estimated against the
        // server's stand-in account and rebuilt for the real address at commit, exactly as a
        // single transfer provider's is. Without a stand-in the quote is shown without a fee.
        val sendTransactionData = route.stubDepositAddress?.takeIf { it.isNotBlank() }?.let { stub ->
            depositTransactionData(tokenIn, amountIn, stub, estimationAttachment(tokenIn))
        }

        // As for a single swap, a floor exists only where the server enforces one: the
        // Guaranteed and slippage rows show only when every output's amount is guaranteed.
        val amountOutMin = route.minBuyAmountTotal
        val effectiveSlippage = if (amountOutMin != null) slippage else null

        return SwapFinalQuote(
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            amountOut = outputs.fold(BigDecimal.ZERO) { sum, output -> sum + output.amountOut },
            amountOutMin = amountOutMin,
            sendTransactionData = sendTransactionData,
            priceImpact = null,
            fields = listOfNotNull(DataFieldSlippage.getField(effectiveSlippage)),
            estimatedTime = route.estimatedTime?.total,
            slippage = effectiveSlippage,
            providerSwapId = null,
            fromAsset = assetIn,
            toAsset = assetOut,
            depositAddress = null,
            depositMemo = null,
            approvalSpender = null,
            providerContext = SplitPreviewContext(
                previewToken = previewToken,
                tokenIn = tokenIn,
                amountIn = amountIn,
                recipients = recipients,
                previewedMins = route.outputsByIndex.map { it.minBuyAmount },
            ),
            mevProtectionAllowed = false,
            splitOutputs = outputs,
        )
    }

    // The previewed outputs matched to the recipients by index. Each must name the destination
    // that was sent: a preview paying elsewhere must not reach the confirmation screen.
    private fun previewedOutputs(
        route: UnstoppableAPI.Response.SplitRoute,
        recipients: List<SplitSwapRecipient>,
    ): List<SplitSwapOutput> {
        val outputs = route.outputsByIndex
        requireOneOutputPerRecipient(outputs.map { it.index }, recipients)
        return recipients.mapIndexed { index, recipient ->
            val output = outputs[index]
            requireSameDestination(output.destinationAddress, recipient, index)
            SplitSwapOutput(
                index = index,
                percent = recipient.percent,
                address = recipient.address.hex,
                amountOut = output.expectedBuyAmount ?: BigDecimal.ZERO,
            )
        }
    }

    private fun requireSameDestination(destinationAddress: String?, recipient: SplitSwapRecipient, index: Int) {
        if (destinationAddress != null && destinationAddress != recipient.address.hex) {
            throw IllegalStateException("Output $index names another destination")
        }
    }

    // Exactly one output per recipient, indexed as requested. A missing one leaves a recipient
    // unpaid; a surplus or repeated one would be summed into the split's totals unseen.
    private fun requireOneOutputPerRecipient(sortedIndices: List<Int>, recipients: List<SplitSwapRecipient>) {
        if (sortedIndices != recipients.indices.toList()) {
            throw IllegalStateException("Outputs $sortedIndices do not match ${recipients.size} recipients")
        }
    }

    // Both lists are in recipient order, one entry per recipient.
    private fun requireFloorsKept(previewed: List<BigDecimal?>, committed: List<BigDecimal?>) {
        previewed.forEachIndexed { index, previewedMin ->
            if (previewedMin == null) return@forEachIndexed
            val committedMin = committed.getOrNull(index)
            if (committedMin == null || committedMin < previewedMin) {
                throw IllegalStateException("Commit withdrew the guaranteed amount of output $index")
            }
        }
    }

    // /v3/split/commit for a split previewed here: creates the order, then rebuilds the one
    // transfer for the deposit address the order names.
    private suspend fun commitSplit(finalQuote: SwapFinalQuote, context: SplitPreviewContext): SwapCommitment {
        if (!consumedPreviewTokens.add(context.previewToken)) {
            throw SwapCommitFailedException(SwapCommitFailure.RefreshRequired)
        }

        currentCoroutineContext().ensureActive()

        val commit = try {
            unstoppableAPI.splitCommit(UnstoppableAPI.Request.Commit(context.previewToken))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val commitException = UnstoppableAPI.CommitException.from(e)
            throw SwapCommitFailedException(commitException.toSwapCommitFailure(), commitException)
        }

        val splitUuid = commit.splitUuid
        if (commit.status != "ok" || splitUuid.isNullOrEmpty()) {
            throw SwapCommitFailedException(SwapCommitFailure.Failed, IllegalStateException("Commit answered ${commit.status} without a splitUuid"))
        }

        try {
            val execution = commit.execution ?: throw IllegalStateException("Commit returned no deposit")
            if (execution.method != "transfer") {
                throw IllegalStateException("Commit returned a ${execution.method} execution")
            }
            // The order is funded with exactly the amount the user confirmed, once: an order
            // asking for anything else is left unfunded and lapses.
            val orderAmount = execution.amount?.toBigDecimalOrNull()
                ?: throw IllegalStateException("Commit names no deposit amount")
            if (orderAmount.compareTo(context.amountIn) != 0) {
                throw IllegalStateException("Commit asks for a different deposit amount")
            }
            val depositAddress = execution.depositAddress?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("No deposit address")

            val outputs = committedOutputs(commit, finalQuote.splitOutputs.orEmpty(), context.recipients)
            // Each recipient's floor can only hold or improve on what was previewed: a commit
            // that lowers one, or stops guaranteeing it, is not funded. Judged per output, since
            // an unchanged total could hide one recipient's loss behind another's gain.
            requireFloorsKept(context.previewedMins, commit.outputsByIndex.map { it.minBuyAmount })
            val quote = finalQuote.copy(
                amountOut = outputs.fold(BigDecimal.ZERO) { sum, output -> sum + output.amountOut },
                amountOutMin = commit.minBuyAmountTotal,
                sendTransactionData = depositTransactionData(context.tokenIn, context.amountIn, depositAddress, execution.attachment),
                providerSwapId = splitUuid,
                depositAddress = depositAddress,
                depositMemo = execution
                    .takeIf { context.tokenIn.blockchainType != BlockchainType.Xrp }
                    ?.resolvedMemo(),
                providerContext = null,
                splitOutputs = outputs,
            )
            return SwapCommitment(quote, splitUuid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SwapCommitFailedException) {
            throw e
        } catch (e: Throwable) {
            // The order exists server-side and lapses unfunded; the user gets a fresh quote.
            throw SwapCommitFailedException(SwapCommitFailure.Failed, e)
        }
    }

    // The committed outputs matched to the recipients by index: each must carry its own uuid and
    // name the destination that was previewed. The committed amount replaces the previewed one.
    private fun committedOutputs(
        commit: UnstoppableAPI.Response.SplitCommit,
        previewed: List<SplitSwapOutput>,
        recipients: List<SplitSwapRecipient>,
    ): List<SplitSwapOutput> {
        val outputs = commit.outputsByIndex
        requireOneOutputPerRecipient(outputs.map { it.index }, recipients)
        return recipients.mapIndexed { index, recipient ->
            val output = outputs[index]
            val uuid = output.uuid?.takeIf { it.isNotEmpty() } ?: throw IllegalStateException("Output $index has no uuid")
            requireSameDestination(output.destinationAddress, recipient, index)
            SplitSwapOutput(
                index = index,
                percent = recipient.percent,
                address = recipient.address.hex,
                amountOut = output.expectedBuyAmount ?: previewed.getOrNull(index)?.amountOut ?: BigDecimal.ZERO,
                uuid = uuid,
            )
        }
    }

    override suspend fun reportSplitInboundTxHash(splitUuid: String, txHash: String) {
        unstoppableAPI.splitSigned(UnstoppableAPI.Request.SplitSigned(splitUuid, txHash))
    }

    // The catalog identifier, or the chain-prefixed address of a token the catalog lacks.
    private fun assetIdentifier(token: Token): String =
        assetCatalog.asset(token) ?: throw IllegalStateException("No identifier for ${token.coin.code}")

    // The sell chain hint beside a chain-addressed asset; a catalog identifier names its chain.
    private fun rateChainId(tokenIn: Token): String? =
        if (assetCatalog.entry(tokenIn) == null) USwapAssetMapping.chainId(tokenIn.blockchainType) else null

    // What [commitSplit] needs from the preview: the token to commit, the deposit the transfer
    // is rebuilt for, the recipients the order's outputs are checked against, and each output's
    // previewed floor (in recipient order) the committed ones must keep.
    private class SplitPreviewContext(
        val previewToken: String,
        val tokenIn: Token,
        val amountIn: BigDecimal,
        val recipients: List<SplitSwapRecipient>,
        val previewedMins: List<BigDecimal?>,
    )

    // What [commit] needs from the preview: the token to commit and the route to rebuild a transfer
    // provider's deposit from once the real address arrives.
    private class PreviewContext(
        val previewToken: String,
        val route: UnstoppableAPI.Response.Route,
        val tokenIn: Token,
        val amountIn: BigDecimal,
    )

    // Tokens already handed to /v3/commit.
    private val consumedPreviewTokens: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    // A deposit attachment the chain cannot deliver readably must fail the route rather than
    // be silently kept local (Monero user note) or sent encrypted (Zano comment, shielded
    // Zcash memo): the provider matches the incoming deposit to its order by this identifier,
    // and a deposit it cannot match is typically unrecoverable.
    private fun resolvedDeliverableMemo(
        execution: UnstoppableAPI.Response.Execution,
        blockchainType: BlockchainType,
    ): String? {
        val memo = execution.resolvedMemo() ?: return null
        if (!blockchainType.memoDelivery.deliversAttachment) {
            throw IllegalStateException("Deposit attachment cannot be delivered on $blockchainType")
        }
        return memo
    }

    private fun getSendTransactionData(
        tokenIn: Token,
        amountIn: BigDecimal,
        bestRoute: UnstoppableAPI.Response.Route,
    ): SendTransactionData {
        val blockchainType = tokenIn.blockchainType
        val execution = bestRoute.execution ?: throw IllegalStateException("No execution found")

        if (blockchainType.isEvm) {
            if (execution.method == "transfer") {
                // A plain transfer the app builds itself, so the same code prices the stand-in at
                // preview and the real deposit at commit; the server's unsignedTx is not consumed.
                // EVM carries no memo: an attachment would never reach the provider.
                resolvedDeliverableMemo(execution, blockchainType)
                val depositAddress = execution.depositAddress ?: throw IllegalStateException("No deposit address")

                return buildEvmDepositTransfer?.invoke(tokenIn, amountIn, depositAddress)
                    ?: ChainRegistry[blockchainType]?.depositTransferData(tokenIn, amountIn, depositAddress)
                    ?: throw IllegalStateException("No evm transfer builder for $blockchainType")
            }

            val signable = execution.primarySignable?.takeIf { it.kind == "evm" }
                ?: throw IllegalStateException("No evm tx found")
            val to = signable.to ?: throw IllegalStateException("No tx `to`")
            ThorChainDepositRoute.requireEvmTarget(to, execution)

            val transactionData = EvmTransactionData(
                to = to,
                value = BigInteger((signable.value ?: "0x0").stripHexPrefix(), 16),
                input = (signable.data ?: "0x").hexStringToByteArray()
            )
            ThorChainDepositRoute.requireEvmCalldata(transactionData.input, transactionData.value, execution, tokenIn, amountIn)

            return SendTransactionData.Evm(
                transactionData = transactionData,
                gasLimit = signable.gas?.let { java.math.BigInteger(it.stripHexPrefix(), 16).toLong() },
            )
        }

        when (blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.Litecoin,
            BlockchainType.Dash,
            BlockchainType.ECash,
                -> {
                // A vault deposit (thorchain_deposit) refunds to the transaction's first input and
                // cannot pay a taproot address, so it spends only non-taproot UTXOs and returns
                // change to the first input. Every other route is a plain send.
                val vaultDeposit = ThorChainDepositRoute.isVaultDeposit(execution)

                return SendTransactionData.Btc(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    // Deliverable here: the memo becomes an OP_RETURN output the provider can read.
                    memo = resolvedDeliverableMemo(execution, blockchainType),
                    amount = amountIn,
                    recommendedGasRate = null,
                    minimumSendAmount = null,
                    changeToFirstInput = vaultDeposit,
                    utxoFilters = if (vaultDeposit) VAULT_DEPOSIT_UTXO_FILTERS else UtxoFilters(),
                )
            }

            BlockchainType.Thorchain,
            BlockchainType.Mayachain -> {
                if (execution.method == "transfer") {
                    // A deposit to a provider's own address, as every other chain builds it.
                    // Deliverable here: the bank memo is a plain, publicly readable field.
                    return SendTransactionData.Thorchain.Send(
                        address = execution.depositAddress ?: throw IllegalStateException("No deposit address"),
                        amount = amountIn,
                        memo = resolvedDeliverableMemo(execution, blockchainType).orEmpty(),
                    )
                }

                return when (val transfer = ThorChainDepositRoute.settlementTransfer(execution)) {
                    is ThorChainDepositRoute.SettlementTransfer.Send -> SendTransactionData.Thorchain.Send(
                        address = transfer.inboundAddress,
                        amount = amountIn,
                        memo = transfer.memo,
                    )

                    is ThorChainDepositRoute.SettlementTransfer.Deposit -> SendTransactionData.Thorchain.Deposit(
                        asset = assetIdentifier(tokenIn),
                        amount = amountIn,
                        memo = transfer.memo,
                    )
                }
            }

            BlockchainType.Solana -> {
                if (execution.method == "transfer") {
                    // Built locally like the EVM transfer; a plain SOL / SPL send carries no memo.
                    resolvedDeliverableMemo(execution, blockchainType)
                    return SendTransactionData.Solana.Simple(
                        address = execution.depositAddress ?: throw IllegalStateException("No deposit address"),
                        amount = amountIn,
                    )
                }

                val message = execution.primarySignable?.takeIf { it.kind == "solana" }?.message
                    ?: throw IllegalStateException("No solana tx found")

                return SendTransactionData.Solana.WithRawTransaction(
                    Base64.decode(message, Base64.DEFAULT)
                )
            }

            BlockchainType.Tron -> {
                if (execution.method == "transfer") {
                    // Built locally like the EVM transfer; a simple send cannot carry the
                    // provider's crediting identifier.
                    resolvedDeliverableMemo(execution, blockchainType)
                    return SendTransactionData.Tron.Simple(
                        address = execution.depositAddress ?: throw IllegalStateException("No deposit address"),
                        amount = amountIn,
                    )
                }

                val tx = execution.primarySignable?.takeIf { it.kind == "tron" }?.tx
                    ?: throw IllegalStateException("No tron tx found")

                return SendTransactionData.Tron.WithCreateTransaction(tx.toString())
            }

            BlockchainType.Stellar -> {
                // Server-built XDR envelope (signed_transaction — Axelar ITS' Stellar leg):
                // sign and submit it. Otherwise a deposit-address transfer (P2P providers)
                // with the provider's binding memo.
                execution.primarySignable?.takeIf { it.kind == "stellar" }?.xdr?.let { xdr ->
                    return SendTransactionData.Stellar.WithTransactionEnvelope(xdr)
                }

                // Deliverable here: a Stellar text memo is a plain, publicly readable field of
                // the payment transaction.
                val memo = resolvedDeliverableMemo(execution, blockchainType)
                    ?: throw IllegalStateException("No memo found")

                return SendTransactionData.Stellar.Regular(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    memo = memo,
                    amount = amountIn
                )
            }

            BlockchainType.Ton -> {
                if (execution.method == "transfer") {
                    // Built locally like the EVM transfer. Deliverable here: a TON comment is a
                    // plain, publicly readable field of the message.
                    return SendTransactionData.Ton.Regular(
                        address = execution.depositAddress ?: throw IllegalStateException("No deposit address"),
                        amount = amountIn,
                        memo = resolvedDeliverableMemo(execution, blockchainType),
                    )
                }

                val tx = execution.primarySignable?.takeIf { it.kind == "ton" }?.tx
                    ?: throw IllegalStateException("No ton tx found")

                return SendTransactionData.Ton.SendRequest(JSONObject(tx.toString()))
            }

            BlockchainType.Xrp -> {
                // The XRP payment carries no memo, and a vault deposit without its memo is a
                // deposit the vault cannot match: refused rather than sent.
                if (ThorChainDepositRoute.isVaultDeposit(execution)) {
                    throw IllegalStateException("XRP cannot carry a vault deposit memo")
                }

                // The identifier travels in the Payment's DestinationTag field rather than a
                // memo, so this builds the transfer from the tag — see [resolvedDestinationTag],
                // which refuses any attachment XRP cannot carry there.
                return SendTransactionData.Xrp(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    amount = amountIn,
                    destinationTag = execution.resolvedDestinationTag(),
                )
            }

            BlockchainType.Near -> {
                // A deposit-address transfer: NEAR, or `ft_transfer` for a NEP-141 token. A
                // native NEAR transfer has no memo field, so a route that needs one fails in
                // resolvedDeliverableMemo rather than sending a deposit the provider cannot match.
                return SendTransactionData.Near(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    amount = amountIn,
                    memo = resolvedDeliverableMemo(execution, blockchainType),
                )
            }

            BlockchainType.Zcash -> {
                if (ThorChainDepositRoute.isVaultDeposit(execution)) {
                    val deposit = ThorChainDepositRoute.shieldedMemoDeposit(execution)
                    return SendTransactionData.Zcash.ShieldedMemo(
                        address = deposit.inboundAddress,
                        amount = amountIn,
                        memo = deposit.memo,
                        memoShieldedAddress = deposit.memoShieldedAddress,
                    )
                }

                // Throws on any attachment: the memo would ride shielded (encrypted, unverifiable
                // that the provider reads it) or not at all on a transparent address. Neither
                // delivers the crediting identifier, and a deposit the provider cannot match is
                // unrecoverable.
                return SendTransactionData.Zcash.Regular(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    amount = amountIn,
                    memo = resolvedDeliverableMemo(execution, blockchainType) ?: ""
                )
            }

            BlockchainType.Monero -> {
                // Throws on any attachment: a Monero memo never leaves this device — the adapter
                // stores it as a wallet user note against the tx id, so the provider can never
                // read the identifier it needs to match this deposit.
                return SendTransactionData.Monero(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    amount = amountIn,
                    memo = resolvedDeliverableMemo(execution, blockchainType)
                )
            }

            BlockchainType.Zano -> {
                // Throws on any attachment: the adapter passes a Zano memo as the transfer's
                // "comment", which travels encrypted on-chain — whether the provider decrypts
                // and reads it is unverifiable from here, and the failure mode is lost funds.
                return SendTransactionData.Zano(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    amount = amountIn,
                    memo = resolvedDeliverableMemo(execution, blockchainType)
                )
            }

            else -> Unit
        }

        throw IllegalArgumentException("Not supported blockchainType: $blockchainType")
    }

    companion object {
        const val ID = "USWAP"
        private const val TAG = "USwapProvider"

        // Inputs a THORChain-family vault can refund to, and the input count its observers accept.
        private val VAULT_DEPOSIT_UTXO_FILTERS = UtxoFilters(
            scriptTypes = listOf(ScriptType.P2PKH, ScriptType.P2WPKHSH, ScriptType.P2WPKH),
            maxOutputsCountForInputs = 10,
        )

        // Recorded with every quote and swap so an issue can be pinned to a release.
        private val userAgent: String by lazy {
            "Unstoppable/${App.appConfigProvider.appVersion} (${App.instance.packageName}; Android ${Build.VERSION.SDK_INT})"
        }
    }

    data class SwapQuoteExtraData(val route: UnstoppableAPI.Response.Route) : SwapQuote.ExtraData
}

// Chains where we consume the server-built tx from `execution` (so we send sourceAddress on
// /v3/preview). Everything else builds its own transfer to the deposit address. Stellar is
// server-built on the DEX routes and names the sender either way on a deposit transfer.
private val Token.needsServerBuiltTx: Boolean
    get() = blockchainType.isEvm || blockchainType in setOf(
        BlockchainType.Tron,
        BlockchainType.Ton,
        BlockchainType.Solana,
        BlockchainType.Stellar,
    )
