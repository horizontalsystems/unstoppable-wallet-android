package io.horizontalsystems.walletkit.modules.crosspay

import com.google.gson.Gson
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.MultiSwapProviderRegistry
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapHelper
import io.horizontalsystems.walletkit.modules.multiswap.providers.USwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.math.BigDecimal

/**
 * Committing a CrossPay order: /v2/swap in cross-asset exact-output mode against the one
 * exact-output-capable provider. Execution is a plain transfer to a deposit address, so the
 * multiswap send-transaction machinery does the actual work.
 *
 * Committing is mandatory per user-visible order — every /v2/swap creates a REAL order —
 * and the entry screen's rate is display-only for the same reason.
 */
class CrossPayManager {
    private val gson = Gson()

    val provider: USwapProvider? = resolveProvider()

    // Only a plain-transfer route may proceed: anything else (signed_transaction,
    // thorchain_deposit, …) needs real transaction building and must not silently join.
    private val supportedExecutionType = "transfer"

    /**
     * Quote and commit in one call. Throws [CrossPayError] only — raw server text never
     * leaves this method.
     */
    suspend fun commit(request: CrossPayRequest): CrossPayOrder {
        val provider = provider ?: throw CrossPayError.TokenUnsupported

        if (!provider.supports(request.tokenIn, request.tokenOut)) throw CrossPayError.TokenUnsupported

        // Omitting the refund address forfeits the buffer refund, which lands on the success
        // path too — so a missing one fails the commit rather than proceeding without.
        // Zcash refunds go to the unified address, exactly as ZEC-in swaps set it.
        val refundAddress = try {
            refundAddress(request.tokenIn)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw CrossPayError.CommitFailed(e)
        }

        val route = try {
            provider.exactOutputCommit(
                tokenIn = request.tokenIn,
                tokenOut = request.tokenOut,
                amountOut = request.amountOut,
                destinationAddress = request.recipient,
                refundAddress = refundAddress,
                slippage = IMultiSwapProvider.DEFAULT_SLIPPAGE,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw mapCommitError(e)
        }

        val uuid = route.uuid
        if (uuid.isNullOrEmpty()) throw CrossPayError.CommitFailed()

        val execution = route.execution
        if (execution == null || execution.method != supportedExecutionType) {
            throw CrossPayError.CommitFailed()
        }

        // `execution.chain` is sometimes a chain id ("56") and sometimes a name ("bsc"), so
        // only a recognised chain that resolves to a *different* blockchain is treated as a
        // mismatch. An unrecognised string is not evidence of disagreement.
        val executionBlockchainType = execution.chain?.let { USwapProvider.chainIdBlockchainTypes[it] }
        if (executionBlockchainType != null && executionBlockchainType != request.tokenIn.blockchainType) {
            throw CrossPayError.CommitFailed()
        }

        val depositAddress = execution.depositAddress ?: throw CrossPayError.CommitFailed()
        val depositAmount = execution.amount?.toBigDecimalOrNull() ?: throw CrossPayError.CommitFailed()

        val minSellAmount = route.minSellAmount

        // A deposit below the floor is refunded whole and no swap happens.
        if (minSellAmount != null && depositAmount < minSellAmount) {
            throw CrossPayError.CommitFailed()
        }

        // Never falls back to the entered amount: that is the *output* the user asked for,
        // not a value the provider has agreed to deliver.
        val amountOut = route.expectedBuyAmount
        if (amountOut == null || amountOut <= BigDecimal.ZERO) {
            throw CrossPayError.CommitFailed()
        }

        // Exact output: the route must deliver precisely what the user entered — a re-priced
        // amount would silently pay the recipient something else.
        if (amountOut.compareTo(request.amountOut) != 0) {
            throw CrossPayError.CommitFailed()
        }

        // Deliverability is decided here, not left to the confirmation's build: the order is
        // already committed either way, and an undeliverable deposit (unsupported attachment,
        // unsupported chain shape) must surface once as an authored commit error rather than
        // fail on every re-entry into the build.
        try {
            provider.depositTransactionData(request.tokenIn, depositAmount, route)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw CrossPayError.CommitFailed(e)
        }

        return CrossPayOrder(
            request = request,
            depositAmount = depositAmount,
            minSellAmount = minSellAmount,
            amountOut = amountOut,
            providerId = provider.id,
            providerName = provider.title,
            depositAddress = depositAddress,
            providerSwapId = uuid,
            refundAddress = refundAddress,
            estimatedTime = route.estimatedTime?.total,
            committedAt = System.currentTimeMillis(),
            route = route,
        )
    }

    private suspend fun refundAddress(tokenIn: Token): String {
        if (tokenIn.blockchainType == BlockchainType.Zcash) {
            ChainRegistry[BlockchainType.Zcash]?.swapUnifiedReceiveAddress(tokenIn)?.let { return it }
        }
        return SwapHelper.getReceiveAddressForToken(tokenIn)
    }

    private fun mapCommitError(error: Throwable): CrossPayError {
        if (error is HttpException) {
            if (error.code() == 503) return CrossPayError.ProviderSuspended

            // A failed /v2/swap answers with the provider error as its body, carrying the same
            // minimumAmount / maximumAmount / errorCode fields /v2/rate reports in
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
        // The one provider serving exact-output cross-asset routes. Resolved by server id so
        // a second exact-output provider some day means widening this to a fan-out.
        private const val PROVIDER_SERVER_ID = "NEAR"

        fun resolveProvider(): USwapProvider? = MultiSwapProviderRegistry.allProviders
            .filterIsInstance<USwapProvider>()
            .firstOrNull { it.serverProviderId == PROVIDER_SERVER_ID }
    }
}
