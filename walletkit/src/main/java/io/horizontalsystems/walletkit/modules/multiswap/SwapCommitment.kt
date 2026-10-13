package io.horizontalsystems.walletkit.modules.multiswap

/**
 * What a confirmed order resolves to: the quote whose transaction is broadcast and the order id the
 * record tracks by. The quote is the previewed one, or a copy rebuilt for the deposit a transfer
 * provider only named at commit; the previewed instance the screen displays is never written to.
 */
data class SwapCommitment(
    val quote: SwapFinalQuote,
    val providerSwapId: String?,
)

/** Why an order could not be created from a previewed quote. Each case has its own message. */
enum class SwapCommitFailure {
    /** The provider's order was worse than the preview beyond the slippage; the price moved. */
    RateChanged,

    /** The preview expired or was already consumed, or the provider was suspended meanwhile. */
    RefreshRequired,

    /** Anything else: a provider refusal, a malformed answer, a transport failure. */
    Failed,
}

/**
 * The committed deposit was built, but the send service refused it. [message] is the service's
 * own caution, written for the user, so the screen can show it instead of a generic line.
 */
class DepositNotSendableException(message: String) : Exception(message)

/**
 * The order could not be created, or the committed deposit could not be built. The screen shows
 * the reason briefly and loads a fresh quote; it must not present its generic error sheet for it.
 * [detail] is a user-facing reason when there is one; otherwise [failure] picks the message.
 */
class SwapCommitFailedException(
    val failure: SwapCommitFailure,
    cause: Throwable? = null,
) : Exception(cause?.message ?: failure.name, cause) {
    val detail: String? = (cause as? DepositNotSendableException)?.message?.takeIf { it.isNotBlank() }
}
