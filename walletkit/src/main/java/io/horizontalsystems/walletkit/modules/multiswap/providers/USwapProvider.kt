package io.horizontalsystems.walletkit.modules.multiswap.providers

import android.util.Base64
import com.google.gson.JsonElement
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.derivation
import io.horizontalsystems.walletkit.core.isEvm
import io.horizontalsystems.walletkit.core.managers.APIClient
import io.horizontalsystems.walletkit.core.nativeTokenQueries
import io.horizontalsystems.walletkit.modules.multiswap.SwapFinalQuote
import io.horizontalsystems.walletkit.modules.multiswap.SwapQuote
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.EvmTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldRecipient
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldSlippage
import io.horizontalsystems.walletkit.core.stripHexPrefix
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.hexStringToByteArray
import io.horizontalsystems.walletkit.modules.multiswap.action.ISwapProviderAction
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.math.BigDecimal
import java.math.BigInteger

class USwapProvider(
    private val provider: UProvider,
    // Narrows the sell side to sources the host app can actually execute (e.g. a
    // route-calldata provider offered only where contract calls are possible).
    private val supportsSourceToken: (Token) -> Boolean = { true },
    // Whether /swap gets `sourceAddress` for this sell token — the server-built-tx
    // signal. An app that builds the source leg itself (e.g. a deposit transfer
    // batched into its own signer flow) turns it off so the server returns the
    // plain deposit route instead of building and gas-estimating a transaction.
    private val shouldIncludeSourceAddress: (Token) -> Boolean = { it.needsServerBuiltTx },
    // Builds the EVM source leg of a transfer route committed without a server-built
    // tx (sourceAddress omitted): the app supplies the deposit calldata its own
    // signer executes. Without it such routes fail the final quote.
    private val buildEvmDepositTransfer: ((tokenIn: Token, amountIn: BigDecimal, depositAddress: String) -> SendTransactionData.Evm)? = null,
) : IMultiSwapProvider {
    override val id = "u_${provider.id}"
    override val title = provider.title
    override val type = provider.type
    override val amlPrecheck = provider.amlPrecheck
    override val isEvm = provider.isEvm
    override val requireTerms = provider.requireTerms
    override val riskLevel = provider.riskLevel

    override fun isSingleTransactionSwap(tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String) = when (provider) {
        // LI.FI spans both: a same-chain pair is a plain single-tx DEX swap, a cross-chain
        // pair is deposit → bridge → delivery, so the flag depends on the pair, not the provider.
        UProvider.Lifi -> tokenInBlockchainTypeUid == tokenOutBlockchainTypeUid
        else -> provider.isSingleTransactionSwap
    }

    private val unstoppableAPI = APIClient.build(
        App.appConfigProvider.uswapApiBaseUrl,
        mapOf("x-api-key" to App.appConfigProvider.uswapApiKey)
    ).create(UnstoppableAPI::class.java)

    private val blockchainTypes = chainIdBlockchainTypes

    private val chainIdByBlockchainType = blockchainTypes.entries.associate { (k, v) -> v to k }

    private var assetsMap = mapOf<Token, String>()
    private var supportedBlockchainTypes = setOf<BlockchainType>()

    private sealed class ProviderData {
        data class TokenMap(val map: Map<Token, String>) : ProviderData()
        data class ChainIds(val ids: List<String>) : ProviderData()
    }

    override suspend fun start() {
        SwapProviderCacheHelper.getCachedChainIds(id)?.let { chainIds ->
            supportedBlockchainTypes = chainIds.mapNotNull { blockchainTypes[it] }.toSet()
            return
        }

        SwapProviderCacheHelper.getCachedTokenMap(id) { it }?.let { map ->
            assetsMap = map
            return
        }

        when (val data = fetchProviderData()) {
            is ProviderData.TokenMap -> {
                assetsMap = data.map
                SwapProviderCacheHelper.saveTokenMap(id, data.map) { it }
            }
            is ProviderData.ChainIds -> {
                supportedBlockchainTypes = data.ids.mapNotNull { blockchainTypes[it] }.toSet()
                SwapProviderCacheHelper.saveChainIds(id, data.ids)
            }
        }
    }

    private suspend fun fetchProviderData(): ProviderData {
        val response = unstoppableAPI.tokens(provider.id)
        val tokens = response.tokens

        if (tokens.isEmpty()) {
            return ProviderData.ChainIds(response.supportedChainIds)
        }

        val assetsMap = mutableMapOf<Token, String>()
        for (token in tokens) {
            // ZEC.ZECSHIELDED is an internal Exolix routing variant. The app always quotes
            // ZEC.ZEC and lets the server expand it into the shielded route, so skip it here
            // to keep the Zcash native token mapping deterministic.
            if (token.identifier == ZCASH_SHIELDED_ASSET) continue

            val blockchainType = blockchainTypes[token.chainId] ?: continue

            when (blockchainType) {
                BlockchainType.ArbitrumOne,
                BlockchainType.Avalanche,
                BlockchainType.Base,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Ethereum,
                BlockchainType.Optimism,
                BlockchainType.Polygon,
                BlockchainType.Tron,
                BlockchainType.Fantom,
                BlockchainType.Gnosis,
                BlockchainType.ZkSync,
                BlockchainType.RobinhoodChain,
                    -> {
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        TokenType.Eip20(token.address)
                    } else {
                        TokenType.Native
                    }

                    App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                        assetsMap[it] = token.identifier
                    }
                }

                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.Litecoin,
                BlockchainType.Zcash,
                BlockchainType.Dash,
                BlockchainType.ECash,
                    -> {
                    var nativeTokenQueries = blockchainType.nativeTokenQueries

                    // filter out taproot for ltc
                    if (blockchainType == BlockchainType.Litecoin) {
                        nativeTokenQueries = nativeTokenQueries.filterNot {
                            it.tokenType.derivation == TokenType.Derivation.Bip86
                        }
                    }

                    val tokens = App.marketKit.tokens(nativeTokenQueries)
                    tokens.forEach {
                        assetsMap[it] = token.identifier
                    }
                }

                BlockchainType.Solana -> {
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        TokenType.Spl(token.address)
                    } else {
                        TokenType.Native
                    }

                    App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                        assetsMap[it] = token.identifier
                    }
                }

                BlockchainType.Stellar -> {
                    val issuer = token.address
                    val tokenType = if (!issuer.isNullOrBlank()) {
                        // Classic asset: the server sends the issuer in `address` and the
                        // asset code in `ticker` (the Axelar ITS SHX case).
                        token.ticker?.let { TokenType.Asset(it, issuer) }
                    } else {
                        TokenType.Native
                    }

                    tokenType?.let {
                        App.marketKit.token(TokenQuery(blockchainType, it))
                    }?.let {
                        assetsMap[it] = token.identifier
                    }
                }

                BlockchainType.Ton -> {
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        TokenType.Jetton(token.address)
                    } else {
                        TokenType.Native
                    }

                    App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                        assetsMap[it] = token.identifier
                    }
                }

                BlockchainType.Monero -> {
                    App.marketKit.token(TokenQuery(blockchainType, TokenType.Native))?.let {
                        assetsMap[it] = token.identifier
                    }
                }

                BlockchainType.Thorchain,
                BlockchainType.Mayachain -> {
                    if (token.address.isNullOrBlank()) {
                        App.marketKit.token(TokenQuery(blockchainType, TokenType.Native))?.let {
                            assetsMap[it] = token.identifier
                        }
                    }
                }

                BlockchainType.Zano -> {
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        TokenType.ZanoAsset(token.address)
                    } else {
                        TokenType.Native
                    }

                    App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                        assetsMap[it] = token.identifier
                    }
                }

                is BlockchainType.Unsupported -> Unit
            }
        }

        return ProviderData.TokenMap(assetsMap)
    }

    override fun supports(blockchainType: BlockchainType): Boolean {
        // overriding fun supports(tokenFrom: Token, tokenTo: Token) makes this method redundant
        return true
    }

    override fun supports(tokenFrom: Token, tokenTo: Token): Boolean {
        if (!supportsSourceToken(tokenFrom)) return false
        return if (assetsMap.isNotEmpty()) {
            assetsMap.contains(tokenFrom) && assetsMap.contains(tokenTo)
        } else {
            tokenFrom.blockchainType in supportedBlockchainTypes &&
                    tokenTo.blockchainType in supportedBlockchainTypes &&
                    deriveIdentifier(tokenFrom) != null &&
                    deriveIdentifier(tokenTo) != null
        }
    }

    // Raw asset encoding for providers that sync no token list from the server (the
    // /tokens response carries only supportedChainIds), so assets are encoded as raw
    // chain addresses instead of resolved through the asset map.
    private fun deriveIdentifier(token: Token): String? = when (provider) {
        // Solana-only: raw SPL mint encoding, case-sensitive base58 — pass verbatim, never
        // re-cased. The wSOL mint means native SOL server-side. The Solana check also makes
        // `supports()` reject non-Solana pairs (`Native` alone would match any chain).
        UProvider.Jupiter -> if (token.blockchainType != BlockchainType.Solana) {
            null
        } else when (val type = token.type) {
            TokenType.Native -> WSOL_MINT
            // The wSOL TOKEN is not swappable here: the server reads the wSOL mint as native
            // SOL, so SOL→wSOL degenerates to "same asset" (wrapping is not a swap) and
            // X→wSOL would deliver native SOL while the user watches the wSOL token balance.
            is TokenType.Spl -> type.address.takeIf { it != WSOL_MINT }
            else -> null
        }
        // No token list (BARTER-style), but LI.FI is CROSS-CHAIN, so each side must be
        // self-describing — the chain travels with the asset so the server resolves a
        // cross-chain pair without a shared `chainId` hint. EVM token → `<CHAIN>.<contract>`,
        // EVM native → `<CHAIN>.<0xeee…>` sentinel, Solana → `SOL.<mint>` (wSOL = native SOL),
        // Tron → `TRON.TRX` (native) / `TRON.<contract>` (TRC20).
        UProvider.Lifi -> when (token.blockchainType) {
            BlockchainType.Solana -> when (val type = token.type) {
                TokenType.Native -> "SOL.$WSOL_MINT"
                // The wSOL TOKEN is excluded for the same reason as on the JUPITER path above.
                is TokenType.Spl -> type.address.takeIf { it != WSOL_MINT }?.let { "SOL.$it" }
                else -> null
            }
            // Tron is TVM (base58 addresses, not `0x`): the server resolves `TRON.TRX` as native
            // and `TRON.<contract>` as a TRC20 — the EVM `0xeee…` native sentinel does not apply.
            // TRC20 contracts ride the `Eip20` type with a base58 address.
            BlockchainType.Tron -> when (val type = token.type) {
                TokenType.Native -> "TRON.TRX"
                is TokenType.Eip20 -> "TRON.${type.address}"
                else -> null
            }
            else -> lifiChainCodes[token.blockchainType]?.let { code ->
                when (val type = token.type) {
                    is TokenType.Eip20 -> "$code.${type.address}"
                    TokenType.Native -> "$code.0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
                    else -> null
                }
            }
        }
        // Raw EVM address encoding (BARTER's server adapter expects addresses, not identifiers).
        else -> when (val type = token.type) {
            is TokenType.Eip20 -> type.address
            TokenType.Native if token.blockchainType.isEvm -> "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
            else -> null
        }
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
    ): SwapQuote {
        val bestRoute = rateBestRoute(tokenIn, tokenOut, amountIn, BigDecimal("1"))

        val approvalAddress = bestRoute.approvalSpenderOrExecution
        val actionRequired = approvalAddress?.let { approvalAddress ->
            val plugin = ChainRegistry[tokenIn.blockchainType]
            val allowance = plugin?.eip20Allowance(tokenIn, approvalAddress)
            plugin?.eip20ApproveAction(allowance, amountIn, approvalAddress, tokenIn)
        }

        return SwapQuote(
            amountOut = bestRoute.expectedBuyAmountOrZero,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            actionRequired = actionRequired,
            estimationTime = bestRoute.estimatedTime?.total,
            extraData = SwapQuoteExtraData(bestRoute)
        )
    }

    // /v2/rate — read-only price/route comparison narrowed to this provider. Picks the route
    // with the best expectedBuyAmount. On an Exolix ZEC pair it additionally fans out a
    // shielded (ZEC.ZECSHIELDED) variant and keeps the better-priced one; the winning
    // sell/buy asset travels back on the route so commitSwap can replay the exact variant.
    private suspend fun rateBestRoute(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        slippage: BigDecimal,
    ): UnstoppableAPI.Response.Route {
        val usingDerivedIdentifiers = assetsMap.isEmpty()
        val assetIn = assetsMap[tokenIn] ?: deriveIdentifier(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn")
        val assetOut = assetsMap[tokenOut] ?: deriveIdentifier(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut")
        val chainId = if (usingDerivedIdentifiers) chainIdByBlockchainType[tokenIn.blockchainType] else null

        val request = UnstoppableAPI.Request.Rate(
            sellAsset = assetIn,
            buyAsset = assetOut,
            sellAmount = amountIn.toPlainString(),
            slippage = slippage,
            providers = setOf(provider.id),
            chainId = chainId,
        )
        var bestRoute = unstoppableAPI.rate(request).routes.maxBy { it.expectedBuyAmountOrZero }

        if (provider == UProvider.Exolix) {
            val requestAlternate = when {
                tokenIn.blockchainType == BlockchainType.Zcash -> {
                    request.copy(sellAsset = ZCASH_SHIELDED_ASSET)
                }

                tokenOut.blockchainType == BlockchainType.Zcash -> {
                    request.copy(buyAsset = ZCASH_SHIELDED_ASSET)
                }

                else -> null
            }

            if (requestAlternate != null) {
                try {
                    val bestRouteAlternate = unstoppableAPI.rate(requestAlternate).routes.maxBy { it.expectedBuyAmountOrZero }
                    if (bestRouteAlternate.expectedBuyAmountOrZero >= bestRoute.expectedBuyAmountOrZero) {
                        bestRoute = bestRouteAlternate
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {

                }
            }
        }

        return bestRoute
    }

    // /v2/swap — commits the order with this single provider and returns the executable
    // route (execution + uuid). `sellAsset`/`buyAsset` replay the variant the rate quote
    // chose (Exolix ZEC). A committed route with no uuid can't be tracked, so reject it
    // before the user sends funds rather than create an untrackable swap.
    private suspend fun commitSwap(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        slippage: BigDecimal,
        destinationAddress: String,
        sourceAddress: String?,
        refundAddress: String?,
        sellAsset: String?,
        buyAsset: String?,
    ): UnstoppableAPI.Response.Route {
        val usingDerivedIdentifiers = assetsMap.isEmpty()
        val assetIn = sellAsset ?: assetsMap[tokenIn] ?: deriveIdentifier(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn")
        val assetOut = buyAsset ?: assetsMap[tokenOut] ?: deriveIdentifier(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut")
        val chainId = if (usingDerivedIdentifiers) chainIdByBlockchainType[tokenIn.blockchainType] else null

        val request = UnstoppableAPI.Request.Swap(
            sellAsset = assetIn,
            buyAsset = assetOut,
            sellAmount = amountIn.toPlainString(),
            slippage = slippage,
            provider = provider.id,
            destinationAddress = destinationAddress,
            refundAddress = refundAddress,
            sourceAddress = sourceAddress,
            chainId = chainId,
        )
        val route = unstoppableAPI.swap(request)

        if (route.uuid.isNullOrEmpty()) {
            throw IllegalStateException("Swap is not trackable (no uuid)")
        }

        return route
    }

    override suspend fun checkAmlAddresses(addresses: List<String>): Boolean? {
        return unstoppableAPI.checkAddresses(addresses.joinToString(",")).passedAmlCheck
    }

    // ----- Private send (same-asset confidential transfer) -----

    // The server-side id ("NEAR_CONFIDENTIAL"), matched against GET /providers by the
    // private send stack. `id` is the app-side "u_…" form used in swap records.
    val serverProviderId: String get() = provider.id

    fun supportsPrivateSend(token: Token) = supports(token, token)

    // /v2/rate in exact-output mode: same asset on both sides, buyAmount = what the recipient
    // must receive. Returns the whole response so the caller can read providerErrors when no
    // route comes back.
    suspend fun privateSendRate(
        token: Token,
        amountOut: BigDecimal,
        slippage: BigDecimal,
    ): UnstoppableAPI.Response.Rate {
        val asset = assetsMap[token] ?: deriveIdentifier(token) ?: throw IllegalStateException("No identifier for token")
        val chainId = if (assetsMap.isEmpty()) chainIdByBlockchainType[token.blockchainType] else null

        return unstoppableAPI.rate(
            UnstoppableAPI.Request.Rate(
                sellAsset = asset,
                buyAsset = asset,
                slippage = slippage,
                providers = setOf(provider.id),
                chainId = chainId,
                buyAmount = amountOut.toPlainString(),
            )
        )
    }

    // /v2/swap in exact-output mode. `sourceAddress` is deliberately never sent: it is optional
    // for transfer providers, and handing the provider the sender's address defeats the point
    // of a private send. The committed route's execution then carries the deposit address and
    // the exact amount to transfer.
    suspend fun privateSendCommit(
        token: Token,
        amountOut: BigDecimal,
        destinationAddress: String,
        refundAddress: String,
        slippage: BigDecimal,
    ): UnstoppableAPI.Response.Route {
        val asset = assetsMap[token] ?: deriveIdentifier(token) ?: throw IllegalStateException("No identifier for token")
        val chainId = if (assetsMap.isEmpty()) chainIdByBlockchainType[token.blockchainType] else null

        val route = unstoppableAPI.swap(
            UnstoppableAPI.Request.Swap(
                sellAsset = asset,
                buyAsset = asset,
                slippage = slippage,
                provider = provider.id,
                destinationAddress = destinationAddress,
                refundAddress = refundAddress,
                chainId = chainId,
                buyAmount = amountOut.toPlainString(),
            )
        )

        if (route.uuid.isNullOrEmpty()) {
            throw IllegalStateException("Swap is not trackable (no uuid)")
        }

        return route
    }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: SwapQuote,
        recipient: io.horizontalsystems.walletkit.entities.Address?,
        slippage: BigDecimal,
    ): SwapFinalQuote {
        val selectedRoute = (swapQuote.extraData as? SwapQuoteExtraData)?.route
        val destination = when {
            recipient != null -> recipient.hex

            selectedRoute?.buyAsset == ZCASH_SHIELDED_ASSET -> {
                ChainRegistry[BlockchainType.Zcash]?.swapUnifiedReceiveAddress(tokenOut)
                    ?: throw IllegalStateException("Zcash support is not available")
            }

            else -> SwapHelper.getReceiveAddressForToken(tokenOut)
        }

        // sourceAddress is the build signal — send it only for chains whose server-built tx
        // we actually consume (by default EVM/Tron/TON/Solana), where it is also required
        // for the signed_transaction `from`. For UTXO/Zcash/Monero/Zano/Stellar (and any
        // token the host app opts out of) we omit it and build the transfer ourselves.
        val sourceAddress = if (shouldIncludeSourceAddress(tokenIn)) {
            SwapHelper.getSendingAddressForToken(tokenIn)
        } else {
            null
        }
        val refundAddress = SwapHelper.getReceiveAddressForToken(tokenIn)

        val bestRoute = commitSwap(
            tokenIn,
            tokenOut,
            amountIn,
            slippage,
            destination,
            sourceAddress,
            refundAddress,
            selectedRoute?.sellAsset,
            selectedRoute?.buyAsset
        )

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

        return SwapFinalQuote(
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            amountOut = amountOut,
            amountOutMin = amountOutMin,
            sendTransactionData = getSendTransactionData(
                tokenIn,
                amountIn,
                bestRoute
            ),
            priceImpact = null,
            fields = fields,
            estimatedTime = bestRoute.estimatedTime?.total,
            slippage = effectiveSlippage,
            providerSwapId = bestRoute.uuid,
            fromAsset = assetsMap[tokenIn] ?: deriveIdentifier(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn"),
            toAsset = assetsMap[tokenOut] ?: deriveIdentifier(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut"),
            depositAddress = bestRoute.execution?.resolvedDepositAddress(),
            depositMemo = bestRoute.execution?.resolvedMemo(),
            approvalSpender = bestRoute.approvalSpenderOrExecution,
        )
    }

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
            val signable = execution.primarySignable?.takeIf { it.kind == "evm" }

            if (signable == null) {
                // A transfer route committed without sourceAddress carries no
                // server-built tx by design — the app builds the deposit transfer.
                // Any other signable-less response is malformed and must fail.
                val depositAddress = execution.takeIf { it.method == "transfer" }?.depositAddress
                if (buildEvmDepositTransfer != null && depositAddress != null &&
                    !shouldIncludeSourceAddress(tokenIn)
                ) {
                    return buildEvmDepositTransfer.invoke(tokenIn, amountIn, depositAddress)
                }
                throw IllegalStateException("No evm tx found")
            }

            val transactionData = EvmTransactionData(
                to = signable.to ?: throw IllegalStateException("No tx `to`"),
                value = BigInteger((signable.value ?: "0x0").stripHexPrefix(), 16),
                input = (signable.data ?: "0x").hexStringToByteArray()
            )

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
                if (!provider.supportsSimpleUtxoTransactions) {
                    throw IllegalStateException("Only simple BTC tx providers are supported")
                }

                return SendTransactionData.Btc(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    // Deliverable here: the memo becomes an OP_RETURN output the provider can read.
                    memo = resolvedDeliverableMemo(execution, blockchainType),
                    amount = amountIn,
                    recommendedGasRate = null,
                    minimumSendAmount = null,
                    changeToFirstInput = false,
                    utxoFilters = UtxoFilters(),
                )
            }

            BlockchainType.Solana -> {
                val message = execution.primarySignable?.takeIf { it.kind == "solana" }?.message
                    ?: throw IllegalStateException("No solana tx found")

                return SendTransactionData.Solana.WithRawTransaction(
                    Base64.decode(message, Base64.DEFAULT)
                )
            }

            BlockchainType.Tron -> {
                val tx = execution.primarySignable?.takeIf { it.kind == "tron" }?.tx

                if (tx == null) {
                    // Same opt-out contract as the EVM branch: a bare transfer route
                    // maps to the plain-transfer shape, but only without a memo — a
                    // simple send cannot carry the provider's crediting identifier.
                    val depositAddress = execution.takeIf { it.method == "transfer" }?.depositAddress
                    if (depositAddress != null && execution.resolvedMemo() == null &&
                        !shouldIncludeSourceAddress(tokenIn)
                    ) {
                        return SendTransactionData.Tron.Simple(depositAddress, amountIn)
                    }
                    throw IllegalStateException("No tron tx found")
                }

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
                val tx = execution.primarySignable?.takeIf { it.kind == "ton" }?.tx
                    ?: throw IllegalStateException("No ton tx found")

                return SendTransactionData.Ton.SendRequest(JSONObject(tx.toString()))
            }

            BlockchainType.Zcash -> {
                if (!provider.supportsSimpleUtxoTransactions) {
                    throw IllegalStateException("Only simple ZEC tx providers are supported")
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
                if (!provider.supportsSimpleUtxoTransactions) {
                    throw IllegalStateException("Only simple XMR tx providers are supported")
                }

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
                if (!provider.supportsSimpleUtxoTransactions) {
                    throw IllegalStateException("Only simple ZANO tx providers are supported")
                }

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
        // The server's chain identifiers. Shared: instances resolve tokens through it, and the
        // private send stack uses it to recognise a committed route's execution.chain — which is
        // sometimes a chain id ("56") and sometimes a name ("bsc"), so only a recognised chain
        // resolving to a DIFFERENT blockchain counts as a mismatch there.
        val chainIdBlockchainTypes = mapOf(
            "43114" to BlockchainType.Avalanche,
            "10" to BlockchainType.Optimism,
            "8453" to BlockchainType.Base,
            "728126428" to BlockchainType.Tron,
            "42161" to BlockchainType.ArbitrumOne,
            "56" to BlockchainType.BinanceSmartChain,
            "solana" to BlockchainType.Solana,
            "137" to BlockchainType.Polygon,
            "bitcoin" to BlockchainType.Bitcoin,
            "1" to BlockchainType.Ethereum,
            "zcash" to BlockchainType.Zcash,
            "bitcoincash" to BlockchainType.BitcoinCash,
            "litecoin" to BlockchainType.Litecoin,
            "stellar" to BlockchainType.Stellar,
            "ton" to BlockchainType.Ton,
            "dash" to BlockchainType.Dash,
            "ecash" to BlockchainType.ECash,
            "monero" to BlockchainType.Monero,
            "zano" to BlockchainType.Zano,
            "100" to BlockchainType.Gnosis,
            "4663" to BlockchainType.RobinhoodChain,
//            "" to BlockchainType.Fantom,
//            "" to BlockchainType.ZkSync,
        )

        // Exolix's shielded Zcash route. Internal routing detail — the app always quotes ZEC.ZEC
        // and lets the server expand it into this shielded variant.
        private const val ZCASH_SHIELDED_ASSET = "ZEC.ZECSHIELDED"

        // Wrapped-SOL mint — the JUPITER server adapter reads it as native SOL.
        private const val WSOL_MINT = "So11111111111111111111111111111111111111112"

        // LI.FI has no token list, so assets are encoded self-describingly as `<CHAIN>.<address>`
        // (see `deriveIdentifier`). This maps each supported EVM chain to the server's chain
        // code — the prefix the server's LI.FI resolver expects. Solana and Tron are handled
        // inline (`SOL.` / `TRON.` prefixes) since their address formats aren't the EVM
        // `0xeee…`/contract shape.
        private val lifiChainCodes = mapOf(
            BlockchainType.Ethereum to "ETH",
            BlockchainType.Polygon to "POL",
            BlockchainType.ArbitrumOne to "ARB",
            BlockchainType.Optimism to "OP",
            BlockchainType.Base to "BASE",
            BlockchainType.Avalanche to "AVAX",
            BlockchainType.BinanceSmartChain to "BSC",
            BlockchainType.RobinhoodChain to "ROBINHOOD",
        )
    }

    data class SwapQuoteExtraData(val route: UnstoppableAPI.Response.Route) : SwapQuote.ExtraData
}

interface UnstoppableAPI {
    @GET("providers")
    suspend fun providers(): List<Response.Provider>

    @GET("tokens")
    suspend fun tokens(
        @Query("provider") provider: String
    ): Response.Tokens

    // /v2/rate — read-only, prices the swap across the requested providers. Returns
    // { routes: [...] } with economics only — no execution, no uuid.
    @POST("rate")
    suspend fun rate(
        @Body request: Request.Rate,
    ): Response.Rate

    // /v2/swap — commits against ONE provider. Creates the order and returns the single
    // executable route DIRECTLY (no { routes } wrapper), now carrying execution + uuid.
    @POST("swap")
    suspend fun swap(
        @Body request: Request.Swap,
    ): Response.Route

    // /v2/track — our recorded swaps, tracked by the route's uuid alone (the server resolves
    // the provider and every swap detail from the record).
    @POST("track")
    suspend fun track(
        @Body request: Request.Track,
    ): Response.Track

    // /v2/track/evm — stateless on-chain reader for native EVM swaps (1inch/Uniswap/Pancake)
    // that were not created through /v2/swap, so there is no record to look up by uuid.
    @POST("track/evm")
    suspend fun trackEvm(
        @Body request: Request.Track,
    ): Response.Track

    // /v2/track/thorchain — stateless on-chain reader for native THORChain/Mayachain swaps.
    @POST("track/thorchain")
    suspend fun trackThorchain(
        @Body request: Request.Track,
    ): Response.Track

    @GET("check-addresses")
    suspend fun checkAddresses(
        @Query("addresses") addresses: String,
    ): Response.CheckAddresses

    object Request {
        // /v2/rate request — compare routes; narrow the fan-out to a single provider.
        // Exactly one of sellAmount / buyAmount must be set — neither or both is a 400.
        // buyAmount asks the route to price backwards from an exact output.
        data class Rate(
            val sellAsset: String,
            val buyAsset: String,
            val sellAmount: String? = null,
            val slippage: BigDecimal,
            val providers: Set<String>,
            val chainId: String? = null,
            val buyAmount: String? = null,
        )

        // /v2/swap request — commit with the single provider. `sourceAddress` is the build
        // signal: supply it and the server returns a ready-to-sign tx; omit it and we build
        // the tx ourselves (UTXO/Zcash/Monero/Zano/Stellar). Same one-of contract for
        // sellAmount / buyAmount as on Rate.
        data class Swap(
            val sellAsset: String,
            val buyAsset: String,
            val sellAmount: String? = null,
            val slippage: BigDecimal,
            val provider: String,
            val destinationAddress: String,
            val refundAddress: String? = null,
            val sourceAddress: String? = null,
            val chainId: String? = null,
            val buyAmount: String? = null,
        )

        data class Track(
            // Recorded swaps (/v2/track): the route's uuid resolves provider + all details.
            val uuid: String? = null,
            // Broadcast tx hash — required for DEX swaps, harmless for P2P/NEAR.
            val inboundTxHash: String? = null,
            // Stateless readers (/v2/track/evm, /v2/track/thorchain) carry full context.
            val provider: String? = null,
            val hash: String? = null,
            val chainId: String? = null,
            val fromAsset: String? = null,
            val fromAddress: String? = null,
            val fromAmount: String? = null,
            val toAsset: String? = null,
            val toAddress: String? = null,
            val toAmount: String? = null,
            val depositAddress: String? = null,
            // Debug-only: forces the server to return an action_required swap. Null in release builds.
            val testActionRequired: Boolean? = null,
        )
    }

    object Response {
        data class Provider(
            val provider: String,
            val name: String? = null,
            val supportedChainIds: List<String> = emptyList(),
            val amlPolicy: String? = null,
            val amlPolicyDescription: String? = null,
            val contacts: Contacts? = null,
            // Whole-provider kill switch.
            val suspended: Boolean = false,
            // How this provider's routes execute: signed_transaction | transfer | …
            // Read by the private send stack, which only accepts plain-transfer providers.
            val executionType: String? = null,
            // Marks a provider routing through a confidential rail; such providers are never
            // offered on the ordinary swap screen. The live server sends a string level
            // ("none", "basic"); the originally documented shape was an object
            // ({"confidential": true}). Modeled as a JsonElement and interpreted in
            // [isConfidential] so either wire form works and neither can break the shared
            // /providers parse (this DTO also serves the suspensions sync).
            val privacy: JsonElement? = null,
            // Scoped restrictions — asset, chain or directed pair. See SwapSuspension. Nullable
            // because the field is absent for the many providers that carry no rules, and Gson
            // fills absent fields with null rather than the declared default.
            val suspensions: List<SwapSuspension>? = null,
        ) {
            data class Contacts(
                val email: String? = null,
                val telegram: String? = null,
                val twitter: String? = null,
                val website: String? = null,
            )

            // Any privacy level other than "none" marks the rail as confidential; the object
            // form reports it explicitly.
            val isConfidential: Boolean
                get() {
                    val privacy = privacy ?: return false
                    return when {
                        privacy.isJsonPrimitive ->
                            privacy.asJsonPrimitive.isString &&
                                    privacy.asString.isNotEmpty() &&
                                    privacy.asString != "none"

                        privacy.isJsonObject ->
                            privacy.asJsonObject.get("confidential")
                                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }
                                ?.asBoolean == true

                        else -> false
                    }
                }
        }

        data class Tokens(
            val tokens: List<Token>,
            val supportedChainIds: List<String> = emptyList(),
        )

        data class Token(
            val chain: String,
            val chainId: String,
            val address: String?,
            val identifier: String,
            // Asset code for chains whose assets are identified by code + issuer (Stellar:
            // `address` carries the issuer, `ticker` the code).
            val ticker: String? = null,
        )

        // /v2/rate response — a list of routes to compare. Each route carries economics
        // only (no execution, no uuid — those appear after committing with /v2/swap).
        // A provider that declined to route lands in providerErrors instead of routes.
        data class Rate(
            val routes: List<Route>,
            val providerErrors: List<ProviderError>? = null,
        )

        // A per-provider refusal. A failed /v2/rate answers 200 with { routes, providerErrors };
        // a failed /v2/swap answers a non-2xx with this same field set as the body itself
        // ({ error, provider, errorCode?, minimumAmount?, maximumAmount? }). Tracking /
        // diagnostics only — never shown in the UI verbatim.
        data class ProviderError(
            val provider: String? = null,
            val providerId: String? = null,
            // The documented key is "error" on both surfaces; "message" is what some
            // providers actually send, so both are accepted.
            val message: String? = null,
            val error: String? = null,
            val errorCode: String? = null,
            val minimumAmount: BigDecimal? = null,
            val maximumAmount: BigDecimal? = null,
        ) {
            val resolvedProviderId: String?
                get() = provider ?: providerId

            val resolvedMessage: String?
                get() = message ?: error
        }

        // A single route. From /rate it is economics-only; from /swap it additionally
        // carries an `execution` block and a top-level `uuid` tracking handle.
        data class Route(
            val sellAsset: String?,
            val buyAsset: String?,
            val expectedBuyAmount: BigDecimal?,
            // The ENFORCED floor the route can deliver (v2 sends an explicit `null` when the
            // amount is only an estimate — floating-rate P2P, re-priced at deposit). null ⇒ no
            // guarantee: the confirm page must not render a "Guaranteed" row.
            val minBuyAmount: BigDecimal?,
            val estimatedTime: EstimatedTime?,
            // EVM ERC20 spender to approve before swapping (1inch/Barter/Circle). On a rate
            // route it is top-level; on a committed route it rides execution.approval.spender.
            val approvalSpender: String?,
            // Present only on a committed (/v2/swap) route — tells you how to send funds.
            val execution: Execution?,
            // v2 tracking handle (swap_records.uuid), top-level on the committed response.
            val uuid: String?,
            // The provider(s) that produced this route. On a /v2/rate fan-out each route
            // carries exactly one — how a multi-provider request (StellarSwapProvider's
            // waterfall) knows which provider to commit with.
            val providers: List<String>? = null,
            // In exact-output mode this is the answer: the amount to deposit, carrying the
            // slippage buffer over meta.near.minSellAmount.
            val sellAmount: BigDecimal? = null,
            val meta: Meta? = null,
        ) {
            // should be getter, otherwise it will be null when restored from json
            val expectedBuyAmountOrZero: BigDecimal
                get() = expectedBuyAmount ?: BigDecimal.ZERO

            val providerId: String?
                get() = providers?.firstOrNull()

            // The ERC20 spender used to compute allowance, wherever it lives on the route.
            val approvalSpenderOrExecution: String?
                get() = approvalSpender ?: execution?.approvalSpender

            data class EstimatedTime(
                val total: Long
            )

            // Exact-output metadata. minSellAmount is the floor below which the deposit is
            // refunded whole and no swap happens.
            data class Meta(
                val near: Near? = null,
            ) {
                data class Near(
                    val minSellAmount: BigDecimal? = null,
                    val exactOutput: Boolean? = null,
                )
            }

            val minSellAmount: BigDecimal?
                get() = meta?.near?.minSellAmount
        }

        // /v2 `execution` discriminated union — switch on `method`. Modeled as one flat
        // class (Gson-friendly) with accessors that read only the fields the method uses.
        data class Execution(
            val method: String,        // signed_transaction | transfer | thorchain_deposit
            val chain: String?,
            // signed_transaction
            val transactions: List<SignableTx>?,
            val approval: Approval?,
            // transfer
            val depositAddress: String?,
            val amount: String?,
            val asset: String?,
            val attachment: Attachment?,
            val unsignedTx: SignableTx?,
            // thorchain_deposit
            val protocol: String?,
            val inboundAddress: String?,
            val memo: String?,
            val delivery: Delivery?,
        ) {
            // The single tx a client signs, if any: signed_transaction's first, or the
            // optional unsignedTx on transfer / thorchain delivery (sent only when we
            // supplied a sourceAddress).
            val primarySignable: SignableTx?
                get() = when (method) {
                    "signed_transaction" -> transactions?.firstOrNull()
                    "transfer" -> unsignedTx
                    "thorchain_deposit" -> delivery?.unsignedTx
                    else -> null
                }

            // The deposit address for the address-transfer methods. signed_transaction is
            // tx-only and has none.
            fun resolvedDepositAddress(): String? = when (method) {
                "transfer" -> depositAddress
                "thorchain_deposit" -> inboundAddress
                else -> null
            }

            // The binding memo for an address transfer — the order identifier the provider
            // uses to credit the deposit, which we must echo back or the funds are lost.
            // Every chain we build a transfer for here (Stellar/Zcash/Monero/Zano/UTXO) puts
            // it in the memo field, whether the server typed it `text` (RUNE/GAIA/TON/NEAR)
            // or `destination_tag` (a numeric tag, e.g. a Stellar memo-id), so accept both.
            // The dedicated XRP destination-tag path (where the tag is a separate tx field,
            // not a memo) is not built by this provider, so this can't misroute one.
            fun resolvedMemo(): String? = when (method) {
                "thorchain_deposit" -> memo
                "transfer" -> attachment?.value
                else -> null
            }

            val approvalSpender: String?
                get() = when (method) {
                    "signed_transaction" -> approval?.spender
                    "thorchain_deposit" -> delivery?.approval?.spender
                    else -> null
                }
        }

        // A signable transaction the server built. `kind` tags the shape; each per-chain
        // builder reads the matching field (evm: to/value/data/gas; solana: message;
        // stellar: xdr; utxo: psbt; tron/ton/cosmos/ripple/near: tx).
        data class SignableTx(
            val kind: String,
            // evm
            val to: String?,
            val from: String?,
            val value: String?,
            val data: String?,
            val gas: String?,
            val gasPrice: String?,
            // base64 forms
            val psbt: String?,
            val message: String?,
            val xdr: String?,
            // object forms (cosmos / ripple / ton / tron / near)
            val tx: JsonElement?,
        )

        data class Approval(
            val token: String?,
            val spender: String,
            val amount: String?,
        )

        // transfer.attachment — an order identifier the provider uses to credit the deposit.
        data class Attachment(
            val type: String,   // destination_tag | text
            val value: String,
        )

        // thorchain_deposit.delivery — chain-specific memo binding.
        data class Delivery(
            val kind: String,   // evm_contract_call | utxo_op_return | cosmos_memo
            val router: String?,
            val approval: Approval?,
            val shieldedMemoAddress: String?,
            val unsignedTx: SignableTx?,
        )

        data class CheckAddresses(
            val passedAmlCheck: Boolean?,
            val results: List<AddressResult>,
        ) {
            data class AddressResult(
                val address: String,
                val passed: Boolean,
                val completed: Boolean,
                val error: String? = null,
            )
        }

        data class Track(
            val status: String, // not_started, pending, swapping, completed, refunded, unknown, failed, action_required
            val type: String?,
            val hash: String?,
            val chainId: String?,
            val fromAsset: String?,
            val fromAmount: String?,
            val fromAddress: String?,
            val toAsset: String?,
            val toAmount: String?,
            val toAddress: String?,
            val legs: List<Leg>?,
            val meta: Meta? = null,
        ) {
            data class Leg(
                val type: String,   // "swap" | "native_send"
                val status: String,
                val hash: String?,
                val chainId: String?,
                val fromAsset: String?,
                val fromAmount: String?,
                val fromAddress: String?,
                val toAsset: String?,
                val toAmount: String?,
                val toAddress: String?,
            )

            data class Meta(
                val provider: String?,
                val pauseReason: String?, // "overdue_with_funds" | "aml" | "frozen"
            )
        }
    }
}

// Chains where we consume the server-built tx from `execution` (so we send sourceAddress
// on /v2/swap). Everything else builds its own transfer to the deposit address.
private val Token.needsServerBuiltTx: Boolean
    get() = blockchainType.isEvm || blockchainType in setOf(
        BlockchainType.Tron,
        BlockchainType.Ton,
        BlockchainType.Solana,
    )
