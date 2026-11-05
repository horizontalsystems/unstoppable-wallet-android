package io.horizontalsystems.walletkit.modules.crosspay

import com.google.gson.Gson
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.MultiSwapProviderRegistry
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapProviderIds
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapProviderInfoManager
import io.horizontalsystems.walletkit.modules.multiswap.providers.USwapAssetMapping
import io.horizontalsystems.walletkit.modules.multiswap.providers.USwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import io.horizontalsystems.walletkit.modules.privatesend.PrivateSendManager
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.math.BigDecimal

/**
 * A CrossPay payment in cross-asset exact-output mode. One unnamed exact-output `/v3/rate`
 * answers with every venue that can price backwards; the cheapest plain-transfer route wins and
 * its venue is previewed. The confirmation screen previews the route (priced against the venue's
 * stand-in deposit address); Send commits it and learns the real deposit address. Execution is a
 * locally built deposit transfer, and the multiswap send-transaction machinery does the actual
 * work.
 *
 * Only the commit creates a REAL order, so the preview is re-run freely and the entry screen's
 * rate is display-only.
 */
class CrossPayManager(
    private val provider: USwapProvider = MultiSwapProviderRegistry.uswap,
    private val providerInfoManager: SwapProviderInfoManager = io.horizontalsystems.walletkit.core.App.swapProviderInfoManager,
) {
    private val gson = Gson()

    /**
     * Prices the payment for the confirmation screen without creating an order. Re-run freely;
     * [commit] turns the preview into an order when the user presses Send. Throws [CrossPayError]
     * only — raw server text never leaves this method. No rate-quote fallbacks: minSellAmount and
     * amountOut come from the preview response alone.
     */
    suspend fun preview(request: CrossPayRequest): CrossPayPreview {
        if (!supportsPair(request.tokenIn, request.tokenOut, provider)) throw CrossPayError.TokenUnsupported

        val rate = try {
            provider.exactOutputRate(
                tokenIn = request.tokenIn,
                tokenOut = request.tokenOut,
                amountOut = request.amountOut,
                slippage = IMultiSwapProvider.DEFAULT_SLIPPAGE,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw mapCommitError(e)
        }

        val rateRoute = bestTransferRoute(rate, providerInfoManager)
            ?: reason(rate.providerErrors.orEmpty())?.let { throw it }
            ?: throw CrossPayError.NoRoute
        val venueId = rateRoute.providerId ?: throw CrossPayError.NoRoute
        val venue = providerInfoManager.venue(venueId)

        // Omitting the refund address forfeits the buffer refund, which lands on the success
        // path too — so a missing one fails the preview rather than proceeding without. The
        // address follows the route's own rule, exactly as a swap sets it: for ZEC a unified
        // address where the route names one, the transparent address otherwise.
        val refundAddress = try {
            provider.refundAddress(request.tokenIn, rateRoute.addressRules)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw CrossPayError.CommitFailed(e)
        }

        val route = try {
            provider.exactOutputPreview(
                tokenIn = request.tokenIn,
                tokenOut = request.tokenOut,
                amountOut = request.amountOut,
                destinationAddress = request.recipient,
                refundAddress = refundAddress,
                slippage = IMultiSwapProvider.DEFAULT_SLIPPAGE,
                venueId = venueId,
                // CrossPay is an ordinary swap in a payment-shaped UI: the swap screen's
                // build-signal contract applies, so the server prices the inbound fee line for
                // tx-consuming chains (EVM & co). The deposit itself is built locally either way.
                includeSourceAddress = true,
                addressRules = rateRoute.addressRules,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw mapCommitError(e)
        }

        val previewToken = route.previewToken
        if (previewToken.isNullOrEmpty()) throw CrossPayError.CommitFailed()

        // The deposit transfer is priced against the stand-in so the confirmation screen shows a
        // network fee. A venue that already names the deposit account at preview is priced
        // against that instead; with neither the screen shows no fee, and the deposit is built
        // once the order names its address.
        val stubDepositAddress = route.stubDepositAddress?.takeIf { it.isNotBlank() }
            ?: route.execution?.depositAddress?.takeIf { it.isNotBlank() }

        // Never falls back to the entered amount: in exact-output mode that is the *output*, in a
        // different token. A deposit of nothing funds nothing, whatever floor the venue states.
        val depositAmount = route.sellAmount ?: throw CrossPayError.CommitFailed()
        if (depositAmount <= BigDecimal.ZERO) throw CrossPayError.CommitFailed()

        val minSellAmount = route.minSellAmount

        // A deposit below the floor is refunded whole and no swap happens.
        if (minSellAmount != null && depositAmount < minSellAmount) {
            throw CrossPayError.CommitFailed()
        }

        val amountOut = route.expectedBuyAmount
        if (amountOut == null || amountOut <= BigDecimal.ZERO) {
            throw CrossPayError.CommitFailed()
        }

        // Exact output: a re-priced amount would silently pay the recipient something else, so a
        // preview promising anything but the requested amount is never shown.
        if (amountOut.compareTo(request.amountOut) != 0) {
            throw CrossPayError.CommitFailed()
        }

        return CrossPayPreview(
            request = request,
            previewToken = previewToken,
            provider = provider,
            venueId = venueId,
            venueName = venue.name,
            depositAmount = depositAmount,
            minSellAmount = minSellAmount,
            amountOut = amountOut,
            stubDepositAddress = stubDepositAddress,
            refundAddress = refundAddress,
            estimatedTime = route.estimatedTime?.total,
            previewedAt = System.currentTimeMillis(),
        )
    }

    /**
     * The only call with side effects: creates the order for a previewed route. The deposit
     * address and attachment exist only from here on. Every failure is an authored
     * [CrossPayError]; the token is never retried.
     */
    suspend fun commit(preview: CrossPayPreview): CrossPayOrder {
        val request = preview.request

        val commit = try {
            preview.provider.commitPreview(preview.previewToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw mapCommitError(e)
        }

        val uuid = commit.uuid
        if (uuid.isNullOrEmpty()) throw CrossPayError.CommitFailed()

        val execution = commit.execution
        if (execution == null || execution.method != SwapProviderIds.TRANSFER_EXECUTION_TYPE) {
            throw CrossPayError.CommitFailed()
        }

        // `execution.chain` is sometimes a chain id ("56") and sometimes a name ("bsc"), so
        // only a recognised chain that resolves to a *different* blockchain is treated as a
        // mismatch. An unrecognised string is not evidence of disagreement.
        val executionBlockchainType = execution.chain?.let { USwapAssetMapping.chainIdBlockchainTypes[it] }
        if (executionBlockchainType != null && executionBlockchainType != request.tokenIn.blockchainType) {
            throw CrossPayError.CommitFailed()
        }

        val depositAddress = execution.depositAddress
        if (depositAddress.isNullOrBlank()) throw CrossPayError.CommitFailed()

        // Exact output: the server decides what to deposit, and the committed order is a fresh
        // quote, so the commit's figure is the one to transfer; without one, the preview's figure
        // stands. Every committed amount present must be positive: a zero or negative one means
        // the order has no valid deposit. A commit asking for MORE than the preview would spend
        // beyond what the Send button authorized, so it is refused as a rate change.
        val executionAmount = execution.amount?.let { it.toBigDecimalOrNull() ?: throw CrossPayError.CommitFailed() }
        if (listOfNotNull(executionAmount, commit.sellAmount).any { it <= BigDecimal.ZERO }) {
            throw CrossPayError.CommitFailed()
        }
        val depositAmount = executionAmount ?: commit.sellAmount ?: preview.depositAmount
        if (depositAmount > preview.depositAmount) throw CrossPayError.RateChanged

        // A deposit below the floor is refunded whole and no swap happens.
        val minSellAmount = preview.minSellAmount
        if (minSellAmount != null && depositAmount < minSellAmount) {
            throw CrossPayError.CommitFailed()
        }

        // Never the entered amount: that's the requested output, not what the provider promised.
        // The commit's figure wins; without one, the preview's figure stands.
        val amountOut = commit.expectedBuyAmount ?: preview.amountOut
        if (amountOut <= BigDecimal.ZERO) throw CrossPayError.CommitFailed()

        // Exact output: whichever figure is effective, anything other than the requested amount is
        // refused as a rate change rather than proceeding with a different delivery.
        if (amountOut.compareTo(request.amountOut) != 0) throw CrossPayError.RateChanged

        // Deliverability is decided here, not left to the confirmation's build: the order is
        // already committed either way, and an undeliverable deposit (unsupported attachment,
        // unsupported chain shape) must surface once as an authored commit error rather than
        // fail on every re-entry into the build.
        try {
            preview.provider.depositTransactionData(request.tokenIn, depositAmount, depositAddress, execution.attachment)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw CrossPayError.CommitFailed(e)
        }

        return CrossPayOrder(
            preview = preview,
            depositAmount = depositAmount,
            amountOut = amountOut,
            depositAddress = depositAddress,
            attachment = execution.attachment,
            providerSwapId = uuid,
        )
    }

    private fun mapCommitError(error: Throwable): CrossPayError {
        // A refused commit: the server's discriminator first (the price moved / the token is
        // stale), then the provider refusal it may carry (same fields as a /v3/rate
        // providerError), then the HTTP status.
        if (error is UnstoppableAPI.CommitException) {
            when (error.status) {
                "rate_changed" -> return CrossPayError.RateChanged
                "refresh_required" -> return CrossPayError.PreviewExpired
            }

            error.providerError?.let { providerError ->
                reason(listOf(providerError))?.let { return it }
            }

            return if (error.httpStatus == 503) CrossPayError.ProviderSuspended else CrossPayError.CommitFailed(error)
        }

        if (error is HttpException) {
            if (error.code() == 503) return CrossPayError.ProviderSuspended

            // A failed /v3/preview answers with the provider error as its body, carrying the same
            // minimumAmount / maximumAmount / errorCode fields /v3/rate reports in
            // `providerErrors`. Only the parsed fields are read — the body itself is never
            // rendered, because it is raw server text.
            parseProviderError(error)?.let { providerError ->
                reason(listOf(providerError))?.let { return it }
            }

            if (error.code() == 404) return CrossPayError.NoRoute

            return CrossPayError.NetworkError(error)
        }

        if (error is java.io.IOException) return CrossPayError.NetworkError(error)

        return CrossPayError.CommitFailed(error)
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

    private fun reason(providerErrors: List<UnstoppableAPI.Response.ProviderError>): CrossPayError? {
        val outOfRange = providerErrors.filter { it.errorCode == "amountOutOfRange" }

        outOfRange.mapNotNull { it.minimumAmount }.minOrNull()?.let {
            return CrossPayError.BelowMinimum(it)
        }

        outOfRange.mapNotNull { it.maximumAmount }.maxOrNull()?.let {
            return CrossPayError.AboveMaximum(it)
        }

        val routeLevelCodes = setOf("amountOutOfRange", "routeNotFound")
        val recognised = providerErrors.any { it.errorCode in routeLevelCodes }

        return if (recognised) CrossPayError.NoRoute else null
    }

    companion object {
        /**
         * Whether [token] can be what the recipient receives, independent of any token list:
         * the payout reaches only a bare address, which is not enough on the chains in
         * [PrivateSendManager.payoutUnsupportedChains].
         */
        fun supportsDestination(token: Token): Boolean =
            token.blockchainType !in PrivateSendManager.payoutUnsupportedChains

        fun supportsPair(tokenIn: Token, tokenOut: Token, provider: USwapProvider = MultiSwapProviderRegistry.uswap): Boolean =
            supportsDestination(tokenOut) && provider.supports(tokenIn, tokenOut)

        /**
         * The route to pay with: among the venues that settle as a plain transfer (anything else,
         * signed_transaction or thorchain_deposit, needs real transaction building and must not
         * silently join), the one asking for the smallest deposit.
         */
        fun bestTransferRoute(
            rate: UnstoppableAPI.Response.Rate,
            providerInfoManager: SwapProviderInfoManager,
        ): UnstoppableAPI.Response.Route? = rate.routes.orEmpty()
            .filter { route ->
                val providerId = route.providerId ?: return@filter false
                route.sellAmount != null &&
                    providerInfoManager.executionType(providerId) == SwapProviderIds.TRANSFER_EXECUTION_TYPE
            }
            .minByOrNull { it.sellAmount!! }
    }
}
