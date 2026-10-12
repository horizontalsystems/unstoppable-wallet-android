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

interface IMultiSwapProvider {
    val id: String
    val title: String
    val type: SwapProviderType
    val amlPrecheck: Boolean
        get() = false
    val isEvm: Boolean
        get() = false
    val requireTerms: Boolean
    val riskLevel: RiskLevel
    fun isSingleTransactionSwap(tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String): Boolean
    fun mevProtectionAllowed(tokenIn: Token, tokenOut: Token): Boolean = false

    val titleShort: String
        get() {
            return if (title.length > 10) {
                title.take(7) + "..."
            } else {
                title
            }
        }

    suspend fun start() = Unit

    fun supports(tokenFrom: Token, tokenTo: Token): Boolean {
        return tokenFrom.blockchainType == tokenTo.blockchainType &&
            supports(tokenFrom.blockchainType)
    }

    fun supports(blockchainType: BlockchainType): Boolean
    suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal
    ): SwapQuote

    suspend fun checkAmlAddresses(addresses: List<String>): Boolean? = null

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

    companion object {
        val DEFAULT_SLIPPAGE: BigDecimal = BigDecimal("1")
    }
}

enum class SwapProviderType(val title: String) {
    DEX("DEX"),
    CEX("CEX")
}

enum class RiskLevel(val title: Int, val icon: Int) {
    EXCELLENT(R.string.RiskLevel_Excellent, R.drawable.star_filled_24),
    GOOD(R.string.RiskLevel_Good, R.drawable.shield_check_filled_24),
    FAIR(R.string.RiskLevel_Fair, R.drawable.thumbsup_24)
}
