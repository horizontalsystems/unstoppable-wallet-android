package io.horizontalsystems.walletkit.modules.crosspay

import io.horizontalsystems.walletkit.modules.multiswap.providers.USwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

/**
 * The intent, known when the user taps Review: pay [amountOut] of [tokenOut] (exact output)
 * to [recipient], funded from [tokenIn]. Neither the deposit address nor the amount to
 * transfer exists yet — both are the commit's answer.
 */
data class CrossPayRequest(
    // The account that opened the confirmation. The refund address is derived from whichever
    // account is active at commit time, so the two must still agree by then.
    val accountId: String,
    val tokenIn: Token,
    val tokenOut: Token,
    // The REAL recipient on tokenOut's chain, never a deposit address.
    val recipient: String,
    val amountOut: BigDecimal,
)

/**
 * A previewed route, produced on the confirmation screen without creating an order. The token is
 * handed to the commit when the user presses Send. [stubDepositAddress] is a ready-to-receive
 * account on tokenIn's chain the deposit transfer is priced against — never a destination to
 * send to. Null when the server names none: the payment is then shown without a network fee and
 * the deposit is built only once the order names its address.
 */
data class CrossPayPreview(
    val request: CrossPayRequest,
    val previewToken: String,
    // The provider the route came from: it commits the token, builds the deposit and takes the
    // hash report.
    val provider: USwapProvider,
    // The preview's sellAmount in tokenIn — what the user confirms and what is transferred. Never
    // the entered amount: under exact output that is the *output*, in a different token.
    val depositAmount: BigDecimal,
    // Below this floor the deposit is refunded whole and no swap happens.
    val minSellAmount: BigDecimal?,
    // What the recipient gets, in tokenOut: equal to the requested amount by the exactness check.
    val amountOut: BigDecimal,
    val stubDepositAddress: String?,
    // Non-optional: the refundable buffer lands here.
    val refundAddress: String,
    // Seconds; null just omits the "estimated arrival" row.
    val estimatedTime: Long?,
    val previewedAt: Long,
) {
    // No cross-asset fee figure: subtracting a tokenOut quantity from a tokenIn one is meaningless.
    val refundableBuffer: BigDecimal?
        get() = minSellAmount?.let { (depositAmount - it).max(BigDecimal.ZERO) }
}

/**
 * The committed order, produced when the user presses Send. Everything the commit does not
 * restate is carried over from the preview the user confirmed.
 */
data class CrossPayOrder(
    val preview: CrossPayPreview,
    // The PREVIEW's sellAmount in tokenIn — EXACTLY what to transfer, the figure the user
    // confirmed. The commit may name a lower one; the server refunds the overpayment.
    val depositAmount: BigDecimal,
    // What the recipient gets, in tokenOut.
    val amountOut: BigDecimal,
    val depositAddress: String,
    val attachment: UnstoppableAPI.Response.Attachment?,
    // The tracking uuid.
    val providerSwapId: String,
) {
    val request: CrossPayRequest get() = preview.request

    // App-side provider id ("u_…") — tracking and history rendering only, never shown.
    val providerId: String get() = preview.provider.id
    val providerName: String get() = preview.provider.title
    val refundAddress: String get() = preview.refundAddress
    val estimatedTime: Long? get() = preview.estimatedTime
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
    // Commit refusals, by the server's discriminator: the provider's order was worse than the
    // preview beyond the slippage, or the preview token expired or was already consumed. Both are
    // shown briefly by the confirmation screen, which then loads a fresh preview.
    object RateChanged : CrossPayError()
    object PreviewExpired : CrossPayError()
    // Every internal/protocol failure: each is a "we could not set this up" from the user's
    // side, and naming the internals would not help them act.
    class CommitFailed(override val cause: Throwable? = null) : CrossPayError()
}
