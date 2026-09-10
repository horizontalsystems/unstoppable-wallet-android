package io.horizontalsystems.walletkit.modules.crosspay

import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

/**
 * The intent, known when the user taps Review: pay [amountOut] of [tokenOut] (exact output)
 * to [recipient], funded from [tokenIn]. Neither the deposit address nor the amount to
 * transfer exists yet — both are the commit's answer.
 */
data class CrossPayRequest(
    val tokenIn: Token,
    val tokenOut: Token,
    // The REAL recipient on tokenOut's chain, never a deposit address.
    val recipient: String,
    val amountOut: BigDecimal,
)

/**
 * The committed order, produced by /v2/swap on the confirmation screen.
 */
data class CrossPayOrder(
    val request: CrossPayRequest,
    // execution.amount — EXACTLY what to transfer in tokenIn. Never recompute it, and never
    // substitute the entered amount: under exact output that is the *output*, a structurally
    // different quantity in a different token.
    val depositAmount: BigDecimal,
    // Below this floor the deposit is refunded whole and no swap happens.
    val minSellAmount: BigDecimal?,
    // What the recipient gets, in tokenOut.
    val amountOut: BigDecimal,
    // App-side provider id ("u_…") — tracking and history rendering only, never shown.
    val providerId: String,
    val providerName: String,
    val depositAddress: String,
    // /v2 tracking uuid.
    val providerSwapId: String,
    // Non-optional: the refundable buffer lands here.
    val refundAddress: String,
    // Seconds; null just omits the "estimated arrival" row.
    val estimatedTime: Long?,
    val committedAt: Long,
    // The raw committed route — the deposit transfer is built from its execution.
    val route: UnstoppableAPI.Response.Route,
) {
    val refundableBuffer: BigDecimal?
        get() = minSellAmount?.let { (depositAmount - it).max(BigDecimal.ZERO) }
}

/**
 * Authored reasons a payment cannot be set up. Every case maps to a localized string on the
 * confirmation screen; raw server text is never rendered.
 */
sealed class CrossPayError : Exception() {
    // In the DESTINATION token — that is the amount field the user can act on.
    class BelowMinimum(val minimum: BigDecimal) : CrossPayError()
    class AboveMaximum(val maximum: BigDecimal) : CrossPayError()
    object NoRoute : CrossPayError()
    object ProviderSuspended : CrossPayError()
    class NetworkError(override val cause: Throwable) : CrossPayError()
    object TokenUnsupported : CrossPayError()
    // Every internal/protocol failure: each is a "we could not set this up" from the user's
    // side, and naming the internals would not help them act.
    class CommitFailed(override val cause: Throwable? = null) : CrossPayError()
}
