package io.horizontalsystems.walletkit.modules.privatesend

import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.walletkit.entities.TransactionDataSortMode
import io.horizontalsystems.walletkit.modules.multiswap.providers.USwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

/**
 * The intent, known when the user taps Next on the send screen. Neither the deposit address
 * nor the amount to transfer exists yet — both are the commit's answer.
 *
 * [amountOut] is what the RECIPIENT receives (exact output): with the private send toggle on,
 * the amount field means "the amount the recipient receives".
 */
data class PrivateSendRequest(
    // The account that opened the confirmation. The refund address is derived from whichever
    // account is active at commit time, so the two must still agree by then.
    val accountId: String,
    val token: Token,
    // The REAL recipient, never a deposit address.
    val recipient: String,
    val amountOut: BigDecimal,
)

/**
 * A previewed route, produced on the confirmation screen without creating an order. The token is
 * handed to the commit when the user presses Send. [stubDepositAddress] is a ready-to-receive
 * account on the token's chain the deposit transfer is priced against — never a destination to
 * send to.
 */
data class PrivateSendPreview(
    val request: PrivateSendRequest,
    val previewToken: String,
    // The provider the route came from: it commits the token and takes the hash report.
    val provider: USwapProvider,
    // The preview's sellAmount — the ceiling the Send button authorizes. Never the entered
    // amount: under exact output that is the *output*, a structurally different quantity.
    val depositAmount: BigDecimal,
    // Below this floor the deposit is refunded whole and no swap happens.
    val minSellAmount: BigDecimal?,
    // What the recipient gets.
    val amountOut: BigDecimal,
    val stubDepositAddress: String,
    // Non-optional: the refundable buffer lands here.
    val refundAddress: String,
    // Seconds; null just omits the "estimated arrival" row.
    val estimatedTime: Long?,
    val previewedAt: Long,
) {
    // The route's cost, NOT `depositAmount - amountOut`: the gap up to `depositAmount` is a
    // refundable deposit ceiling, not a price. With `minSellAmount` unknown this over-states
    // rather than under-states, and the confirmation screen raises the buffer-unknown warning
    // alongside it.
    val privateFee: BigDecimal
        get() = ((minSellAmount ?: depositAmount) - amountOut).max(BigDecimal.ZERO)

    val refundableBuffer: BigDecimal?
        get() = minSellAmount?.let { (depositAmount - it).max(BigDecimal.ZERO) }
}

/**
 * The committed order, produced when the user presses Send. Everything the commit does not
 * restate is carried over from the preview the user confirmed.
 */
data class PrivateSendOrder(
    val preview: PrivateSendPreview,
    // The commit's amount — EXACTLY what to transfer, never above the preview's.
    val depositAmount: BigDecimal,
    // What the recipient gets: equal to the requested amount by the exactness check.
    val amountOut: BigDecimal,
    val depositAddress: String,
    val attachment: UnstoppableAPI.Response.Attachment?,
    // The tracking uuid.
    val providerSwapId: String,
) {
    val request: PrivateSendRequest get() = preview.request

    // App-side provider id ("u_…") — tracking and history rendering only, never shown.
    val providerId: String get() = preview.provider.id
    val refundAddress: String get() = preview.refundAddress
    val estimatedTime: Long? get() = preview.estimatedTime
}

/**
 * The bitcoin send screen's settings, carried into the deposit transfer so a private send
 * honours the user's coin control and transaction shaping. A timelock is deliberately NOT
 * carried: the deposit must be spendable by the provider immediately, so the timelock
 * setting is disabled under the Private tab.
 *
 * Derived from the send view model's chain settings rather than serialized with the
 * confirmation page: after process death the confirmation restores with service defaults,
 * which is safe.
 */
data class PrivateSendBtcParams(
    val feeRate: Int?,
    val unspentOutputs: List<UnspentOutputInfo>?,
    val transactionSorting: TransactionDataSortMode?,
    val rbfEnabled: Boolean,
)

/**
 * Authored reasons a private send cannot be set up. Every case maps to a localized string on
 * the confirmation screen; raw server text is never rendered.
 */
sealed class PrivateSendError : Exception() {
    // Named, not merely reported: the user only learns the floor on the confirmation screen
    // and has to go back and change the amount to act on it.
    class BelowMinimum(val minimum: BigDecimal) : PrivateSendError()
    class AboveMaximum(val maximum: BigDecimal) : PrivateSendError()
    object NoRoute : PrivateSendError()
    object ExactOutputUnsupported : PrivateSendError()
    object ProviderSuspended : PrivateSendError()
    class NetworkError(override val cause: Throwable) : PrivateSendError()
    object TokenUnsupported : PrivateSendError()
    // A route requiring a transaction tag this wallet cannot attach on this chain.
    object AttachmentUnsupported : PrivateSendError()
    // Commit refusals, by the server's discriminator: the provider's order was worse than the
    // preview beyond the slippage, or the preview token expired or was already consumed. Both are
    // shown briefly by the confirmation screen, which then loads a fresh preview.
    object RateChanged : PrivateSendError()
    object PreviewExpired : PrivateSendError()
    // Every internal/protocol failure: each is a "we could not set this up" from the user's
    // side, and naming the internals would leak route detail without helping them act.
    class CommitFailed(override val cause: Throwable? = null) : PrivateSendError()
}
