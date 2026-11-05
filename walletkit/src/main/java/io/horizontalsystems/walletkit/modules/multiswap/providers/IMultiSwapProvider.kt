package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.multiswap.SwapCommitFailedException
import io.horizontalsystems.walletkit.modules.multiswap.SwapCommitment
import io.horizontalsystems.walletkit.modules.multiswap.SwapFinalQuote
import io.horizontalsystems.walletkit.modules.multiswap.SwapQuote
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

/**
 * A quote source. One request answers with several venues' routes, so a provider returns a list
 * of quotes per round, each naming its [SwapVenue]; the selected quote is handed back to the same
 * instance for the preview, the AML precheck and the commit.
 */
interface IMultiSwapProvider {
    val id: String

    /** Loads whatever the provider needs before it can quote (a token catalog, a provider list). */
    suspend fun start() = Unit

    /** Whether the pair can be asked for at all; a pair that cannot shows as unsupported before any amount is typed. */
    fun supports(tokenFrom: Token, tokenTo: Token): Boolean

    suspend fun fetchQuotes(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
    ): List<SwapQuote>

    /**
     * The route's AML address precheck, with the `amlProviderId` the quote carries. `true` passed,
     * `false` failed (do not send), null the backend could not process the check.
     */
    suspend fun checkAmlAddresses(addresses: List<String>, amlProviderId: String): Boolean? = null

    /** Whether a typed ZEC recipient for this quote must be a transparent address. */
    fun zcashTransparentRecipientOnly(quote: SwapQuote): Boolean = quote.tokenOut.blockchainType == BlockchainType.Zcash

    /**
     * The confirmation screen's quote. Has no side effects: it is re-requested on every refresh,
     * fee-setting or slippage change, so a provider must not create an order here.
     */
    suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: SwapQuote,
        recipient: Address?,
        slippage: BigDecimal,
    ): SwapFinalQuote

    /**
     * Creates the order for a previewed quote when the user presses Swap. Resolves to the quote
     * whose transaction is broadcast (the same one, or a rebuilt one when a transfer provider
     * learns the deposit address only here) and the order id the record tracks by. Providers
     * without an order step confirm the quote as it is. Throws [SwapCommitFailedException] when
     * the order could not be created; the quote is then stale and must be previewed again.
     */
    suspend fun commit(finalQuote: SwapFinalQuote): SwapCommitment =
        SwapCommitment(finalQuote, finalQuote.providerSwapId)

    /** Reports the broadcast inbound tx hash to the provider's backend. Best-effort. */
    suspend fun reportInboundTxHash(providerSwapId: String, txHash: String) = Unit

    /** A new session for the next swap: called when the swap screen opens and after a swap completes. */
    fun startSession() = Unit

    companion object {
        val DEFAULT_SLIPPAGE: BigDecimal = BigDecimal("1")
    }
}

enum class RiskLevel(val title: Int, val icon: Int) {
    EXCELLENT(R.string.RiskLevel_Excellent, R.drawable.star_filled_24),
    GOOD(R.string.RiskLevel_Good, R.drawable.shield_check_filled_24),
    FAIR(R.string.RiskLevel_Fair, R.drawable.thumbsup_24);

    companion object {
        /** The server's `amlPolicy` value (`excellent` | `good` | `fair`); null for anything else. */
        fun fromAmlPolicy(value: String): RiskLevel? = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
