package io.horizontalsystems.walletkit.modules.privatesend

import com.google.gson.Gson
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapHelper
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapProviderIds
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapProviderInfoManager
import io.horizontalsystems.walletkit.modules.multiswap.providers.USwapAssetCatalog
import io.horizontalsystems.walletkit.modules.multiswap.providers.USwapAssetMapping
import io.horizontalsystems.walletkit.modules.multiswap.providers.USwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.math.BigDecimal

/**
 * The private send stack: which tokens can be sent privately, and committing an order.
 *
 * A private send routes a transfer through a confidential USwap venue so the recipient cannot
 * be linked on-chain to the sender. The confirmation screen previews the route (priced against
 * the venue's stand-in deposit address); Send commits it and learns the real deposit address.
 * Execution is a plain transfer to that address, so the multiswap send-transaction machinery
 * does the actual work.
 *
 * Which venues are confidential comes from `GET /v3/providers` (`privacy`), and which tokens they
 * list from the token catalog, never from a hardcoded id: the server, not a release, decides
 * availability.
 */
class PrivateSendManager(
    private val provider: USwapProvider,
    private val providerInfoManager: SwapProviderInfoManager,
    private val assetCatalog: USwapAssetCatalog,
) {
    private val gson = Gson()
    private val syncMutex = Mutex()

    // Bumped whenever availability data changes (the provider list or the catalog), so an open
    // send screen reveals or hides the toggle without a reload.
    private val _availabilityFlow = MutableStateFlow(0L)
    val availabilityFlow = _availabilityFlow.asStateFlow()

    /**
     * Pure in-memory lookup over already-synced state. Called from the render path: it must
     * never trigger a fetch, block, or become suspend.
     */
    fun isSupported(token: Token): Boolean =
        token.blockchainType !in payoutUnsupportedChains && privateVenueIds(token).isNotEmpty()

    /**
     * Refreshes the provider list and the token catalog (both TTL-guarded). Cheap to call on
     * every send screen open: everything runs on IO and a fresh list is not fetched again for
     * a whole interval.
     */
    suspend fun sync() = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            try {
                provider.start()
            } finally {
                _availabilityFlow.value++
            }
        }
    }

    // The confidential venues that list the token: in the catalog under a provider that is
    // confidential, settles as a plain transfer and is not suspended.
    private fun privateVenueIds(token: Token): List<String> =
        assetCatalog.privateVenueIds(token, providerInfoManager.privateProviderIds)

    /**
     * Prices the private send for the confirmation screen without creating an order: one
     * exact-output `/v3/rate` over the confidential privacy levels chooses the venue (every
     * route delivers the identical requested output, so the cheapest deposit wins), and the
     * preview is made with that venue. Re-run freely; [commit] turns the preview into an order
     * when the user presses Send. Every economic value below prefers the preview response.
     *
     * Throws [PrivateSendError] only — raw server text never leaves this method.
     */
    suspend fun preview(request: PrivateSendRequest): PrivateSendPreview {
        val token = request.token

        if (token.blockchainType in payoutUnsupportedChains) {
            throw PrivateSendError.TokenUnsupported
        }

        val venueIds = privateVenueIds(token)
        if (venueIds.isEmpty()) throw PrivateSendError.TokenUnsupported

        val venueId = if (venueIds.size > 1) {
            selectCheapestVenue(venueIds, token, request.amountOut) ?: venueIds.first()
        } else {
            venueIds.first()
        }

        // Omitting the refund address forfeits the buffer refund, which lands on the success
        // path too — so a missing one fails the preview rather than proceeding without.
        val refundAddress = try {
            SwapHelper.getReceiveAddressForToken(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw PrivateSendError.CommitFailed(e)
        }

        // `sourceAddress` is deliberately never sent: it is optional for transfer venues, and
        // handing the venue the sender's address defeats the point of a private send.
        val route = try {
            provider.exactOutputPreview(
                tokenIn = token,
                tokenOut = token,
                amountOut = request.amountOut,
                destinationAddress = request.recipient,
                refundAddress = refundAddress,
                slippage = IMultiSwapProvider.DEFAULT_SLIPPAGE,
                venueId = venueId,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw mapCommitError(e)
        }

        val previewToken = route.previewToken
        if (previewToken.isNullOrEmpty()) throw PrivateSendError.CommitFailed()

        // The deposit transfer is priced against the stand-in so the confirmation screen shows a
        // network fee. A venue that already names the deposit account at preview is priced
        // against that instead; with neither the screen shows no fee, and the deposit is built
        // once the order names its address.
        val stubDepositAddress = route.stubDepositAddress?.takeIf { it.isNotBlank() }
            ?: route.execution?.depositAddress?.takeIf { it.isNotBlank() }

        // Never falls back to the entered amount: in exact-output mode that is the *output*, a
        // structurally different quantity.
        val depositAmount = route.sellAmount ?: throw PrivateSendError.CommitFailed()
        if (depositAmount <= BigDecimal.ZERO) throw PrivateSendError.CommitFailed()

        val minSellAmount = route.minSellAmount

        // A deposit below the floor is refunded whole and no swap happens.
        if (minSellAmount != null && depositAmount < minSellAmount) {
            throw PrivateSendError.CommitFailed()
        }

        val amountOut = route.expectedBuyAmount
        if (amountOut == null || amountOut <= BigDecimal.ZERO) {
            throw PrivateSendError.CommitFailed()
        }

        // Exact output: a re-priced amount would silently deliver the recipient something else,
        // so a preview promising anything but the requested amount is never shown.
        if (amountOut.compareTo(request.amountOut) != 0) {
            throw PrivateSendError.CommitFailed()
        }

        return PrivateSendPreview(
            request = request,
            previewToken = previewToken,
            provider = provider,
            venueId = venueId,
            depositAmount = depositAmount,
            minSellAmount = minSellAmount,
            amountOut = amountOut,
            stubDepositAddress = stubDepositAddress,
            refundAddress = refundAddress,
            estimatedTime = route.estimatedTime?.total,
            previewedAt = System.currentTimeMillis(),
        )
    }

    // One unnamed exact-output rate over the confidential levels: the venue with the cheapest
    // deposit among those that list the token wins. No route anywhere surfaces an authored
    // reason when a venue named one.
    // Null when the round fails or no listed venue priced the output: the preview then runs on the
    // first venue and reports its own refusal, rather than the comparison refusing for it.
    private suspend fun selectCheapestVenue(venueIds: List<String>, token: Token, amountOut: BigDecimal): String? {
        val rate = try {
            provider.exactOutputRate(
                tokenIn = token,
                tokenOut = token,
                amountOut = amountOut,
                slippage = IMultiSwapProvider.DEFAULT_SLIPPAGE,
                privacy = providerInfoManager.privacyLevelsExcludingNone,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return null
        }

        val ids = venueIds.map { it.uppercase() }
        return rate.routes.orEmpty()
            .filter { route -> route.providerId?.uppercase() in ids && route.sellAmount != null }
            .minByOrNull { it.sellAmount!! }
            ?.providerId
    }

    /**
     * The only call with side effects: creates the order for a previewed route. The deposit
     * address and attachment exist only from here on. Every failure is an authored
     * [PrivateSendError]; the token is never retried.
     */
    suspend fun commit(preview: PrivateSendPreview): PrivateSendOrder {
        val token = preview.request.token

        val commit = try {
            preview.provider.commitPreview(preview.previewToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw mapCommitError(e)
        }

        val uuid = commit.uuid
        if (uuid.isNullOrEmpty()) throw PrivateSendError.CommitFailed()

        val execution = commit.execution
        if (execution == null || execution.method != SwapProviderIds.TRANSFER_EXECUTION_TYPE) {
            throw PrivateSendError.CommitFailed()
        }

        // `execution.chain` is sometimes a chain id ("56") and sometimes a name ("bsc"), so
        // only a recognised chain that resolves to a *different* blockchain is treated as a
        // mismatch. An unrecognised string is not evidence of disagreement.
        val executionBlockchainType = execution.chain?.let { USwapAssetMapping.chainIdBlockchainTypes[it] }
        if (executionBlockchainType != null && executionBlockchainType != token.blockchainType) {
            throw PrivateSendError.CommitFailed()
        }

        // Blank is as unusable as absent: it reaches the deposit build and dies there on address
        // parsing, with a real order already standing behind it.
        val depositAddress = execution.depositAddress
        if (depositAddress.isNullOrBlank()) throw PrivateSendError.CommitFailed()

        // Exact output: the server decides what to deposit, and the committed order is a fresh
        // quote, so the commit's figure is the one to transfer. Every committed amount present
        // must be positive: a zero or negative one means the order has no valid deposit.
        val executionAmount = execution.amount?.let { it.toBigDecimalOrNull() ?: throw PrivateSendError.CommitFailed() }
        val depositAmount = executionAmount ?: commit.sellAmount ?: throw PrivateSendError.CommitFailed()
        if (listOfNotNull(executionAmount, commit.sellAmount).any { it <= BigDecimal.ZERO }) {
            throw PrivateSendError.CommitFailed()
        }

        // The Send button authorized the previewed ceiling and nothing above it: an order that
        // wants more is refused like any other rate change (it expires unfunded).
        if (depositAmount > preview.depositAmount) throw PrivateSendError.RateChanged

        // A deposit below the floor is refunded whole and no swap happens.
        val minSellAmount = preview.minSellAmount
        if (minSellAmount != null && depositAmount < minSellAmount) {
            throw PrivateSendError.CommitFailed()
        }

        // The effective output is the commit's figure, or the preview's when the commit omits it.
        val amountOut = commit.expectedBuyAmount ?: preview.amountOut
        if (amountOut <= BigDecimal.ZERO) throw PrivateSendError.CommitFailed()

        // Exact output: an order delivering anything other than the amount the user asked to send
        // is refused as a rate change rather than proceeding with a different delivery.
        if (amountOut.compareTo(preview.request.amountOut) != 0) throw PrivateSendError.RateChanged

        // Attachment deliverability is decided here, not left to the deposit build: the order
        // is already committed either way, and an undeliverable attachment must surface once
        // as an authored commit error rather than fail on every re-entry into the build.
        PrivateSendDepositBuilder.deliverableMemo(execution.attachment, token.blockchainType)

        return PrivateSendOrder(
            preview = preview,
            depositAmount = depositAmount,
            amountOut = amountOut,
            depositAddress = depositAddress,
            attachment = execution.attachment,
            providerSwapId = uuid,
        )
    }

    private fun mapCommitError(error: Throwable): PrivateSendError {
        // A refused commit: the server's discriminator first (the price moved / the token is
        // stale), then the provider refusal it may carry (same fields as a /v3/rate
        // providerError), then the HTTP status.
        if (error is UnstoppableAPI.CommitException) {
            when (error.status) {
                "rate_changed" -> return PrivateSendError.RateChanged
                "refresh_required" -> return PrivateSendError.PreviewExpired
            }

            error.providerError?.let { providerError ->
                reason(listOf(providerError))?.let { return it }
            }

            return if (error.httpStatus == 503) PrivateSendError.ProviderSuspended else PrivateSendError.CommitFailed(error)
        }

        if (error is HttpException) {
            if (error.code() == 503) return PrivateSendError.ProviderSuspended

            // A failed /v3/preview answers with the provider error as its body, carrying the same
            // minimumAmount / maximumAmount / errorCode fields /v3/rate reports in
            // `providerErrors`. Only the parsed fields are read — the body itself is never
            // rendered, because it is raw server text.
            parseProviderError(error)?.let { providerError ->
                reason(listOf(providerError))?.let { return it }
            }

            if (error.code() == 404) return PrivateSendError.NoRoute

            return PrivateSendError.NetworkError(error)
        }

        if (error is java.io.IOException) return PrivateSendError.NetworkError(error)

        return PrivateSendError.CommitFailed(error)
    }

    private fun parseProviderError(error: HttpException): UnstoppableAPI.Response.ProviderError? {
        val body = try {
            error.response()?.errorBody()?.string()
        } catch (e: Throwable) {
            null
        } ?: return null

        val parsed = try {
            gson.fromJson(body, UnstoppableAPI.Response.ProviderError::class.java)
        } catch (e: Throwable) {
            null
        } ?: return null

        // A body with none of the provider-error markers is some other failure wearing the
        // same envelope (an auth rejection, a proxy page). Returning null lets the caller
        // fall back on the status code instead of inventing a route-level reason from it.
        if (parsed.errorCode == null && parsed.minimumAmount == null && parsed.maximumAmount == null) {
            return null
        }

        return parsed
    }

    // The one mapping from provider errors to authored reasons, shared by the /v3/rate
    // providerErrors array and the single error body of a failed /v3/preview or /v3/commit. Returns null when
    // nothing is recognisable, so each caller decides its own fallback.
    private fun reason(providerErrors: List<UnstoppableAPI.Response.ProviderError>): PrivateSendError? {
        val outOfRange = providerErrors.filter { it.errorCode == "amountOutOfRange" }

        outOfRange.mapNotNull { it.minimumAmount }.minOrNull()?.let {
            return PrivateSendError.BelowMinimum(it)
        }

        outOfRange.mapNotNull { it.maximumAmount }.maxOrNull()?.let {
            return PrivateSendError.AboveMaximum(it)
        }

        val exactOutputDeclined = providerErrors.any {
            it.errorCode == "routeNotFound" &&
                    it.resolvedMessage?.lowercase()?.contains("exact output") == true
        }
        if (exactOutputDeclined) return PrivateSendError.ExactOutputUnsupported

        // An amountOutOfRange that carries no figures is still a route-level refusal, and "no
        // private route for this token and amount" is honest about it. Anything else is not
        // ours to interpret.
        val routeLevelCodes = setOf("amountOutOfRange", "routeNotFound")
        val recognised = providerErrors.any { it.errorCode in routeLevelCodes }

        return if (recognised) PrivateSendError.NoRoute else null
    }

    companion object {
        /**
         * Chains where the provider's payout cannot reach the recipient the way it usually
         * has to. Recipients on XRP and Stellar are mostly exchanges that credit a deposit
         * only by its destination tag (XRP) or memo (Stellar), but the provider pays out to
         * a bare address: the 1Click API takes no tag or memo, and the backend refuses XRP
         * X-addresses (routeNotFound) and Stellar muxed M-addresses. Such a payout arrives
         * but is never credited, or is rejected outright by an XRP account flagged
         * RequireDestTag. Private Send and CrossPay's destination side both read this list.
         */
        val payoutUnsupportedChains = setOf(
            BlockchainType.Xrp,
            BlockchainType.Stellar,
        )
    }
}
