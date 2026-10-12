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
 * The order could not be created, or the committed deposit could not be built. The screen shows
 * the reason briefly and loads a fresh quote; it must not present its generic error sheet for it.
 */
class SwapCommitFailedException(
    val failure: SwapCommitFailure,
    cause: Throwable? = null,
) : Exception(cause?.message ?: failure.name, cause)
