package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.multiswap.SwapFinalQuote
import io.horizontalsystems.bankwallet.modules.multiswap.SwapQuote
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

interface IMultiSwapProvider {
    val id: String
    val title: String
    val icon: Int
    val type: SwapProviderType
    val amlPrecheck: Boolean
        get() = false
    val isEvm: Boolean
        get() = false
    val requireTerms: Boolean
    val riskLevel: RiskLevel
    fun isSingleChainSwap(tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String): Boolean

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

    suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: SwapQuote,
        recipient: Address?,
        slippage: BigDecimal,
    ): SwapFinalQuote

    companion object {
        val DEFAULT_SLIPPAGE: BigDecimal = BigDecimal("1")
    }
}

enum class SwapProviderType(val title: String) {
    DEX("DEX"),
    CEX("CEX")
}

enum class RiskLevel(val title: Int, val icon: Int) {
    AUTO(R.string.RiskLevel_Auto, R.drawable.shield_check_filled_24),
    FLEXIBLE(R.string.RiskLevel_Flexible, R.drawable.thumbsup_24),
    CONTROLLED(R.string.RiskLevel_Controlled, R.drawable.ic_warning_filled_24),
    PRECHECK(R.string.RiskLevel_Precheck, R.drawable.radar_24)
}
