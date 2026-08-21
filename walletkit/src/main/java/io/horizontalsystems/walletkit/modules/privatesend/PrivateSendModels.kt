package io.horizontalsystems.walletkit.modules.privatesend

import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.walletkit.entities.TransactionDataSortMode
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
    val token: Token,
    // The REAL recipient, never a deposit address.
    val recipient: String,
    val amountOut: BigDecimal,
)

/**
 * The committed order, produced by /v2/swap on the confirmation screen.
 */
data class PrivateSendOrder(
    val request: PrivateSendRequest,
    // execution.amount — EXACTLY what to transfer. Never recompute it, and never substitute
    // the entered amount: under exact output that is the *output*, a structurally different
    // quantity.
    val depositAmount: BigDecimal,
    // Below this floor the deposit is refunded whole and no swap happens.
    val minSellAmount: BigDecimal?,
    // What the recipient gets.
    val amountOut: BigDecimal,
    // App-side provider id ("u_…") — tracking and history rendering only, never shown.
    val providerId: String,
    val depositAddress: String,
    val attachment: UnstoppableAPI.Response.Attachment?,
    // /v2 tracking uuid.
    val providerSwapId: String,
    // Non-optional: the refundable buffer lands here.
    val refundAddress: String,
    // Seconds; null just omits the "estimated arrival" row.
    val estimatedTime: Long?,
    val committedAt: Long,
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
 * The bitcoin send screen's settings, carried into the deposit transfer so a private send
 * honours the user's coin control and transaction shaping. A timelock is deliberately NOT
 * carried: the deposit must be spendable by the provider immediately, so the timelock
 * setting is disabled while the toggle is on.
 *
 * Held in memory on [PrivateSendViewModel] rather than serialized with the confirmation
 * page: after process death the confirmation restores with service defaults, which is safe.
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
    // Every internal/protocol failure: each is a "we could not set this up" from the user's
    // side, and naming the internals would leak route detail without helping them act.
    class CommitFailed(override val cause: Throwable? = null) : PrivateSendError()
}
