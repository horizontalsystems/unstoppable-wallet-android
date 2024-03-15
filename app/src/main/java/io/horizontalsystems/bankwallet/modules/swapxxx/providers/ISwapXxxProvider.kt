package io.horizontalsystems.bankwallet.modules.swapxxx.providers

import io.horizontalsystems.bankwallet.modules.swapxxx.ISwapFinalQuote
import io.horizontalsystems.bankwallet.modules.swapxxx.ISwapQuote
import io.horizontalsystems.bankwallet.modules.swapxxx.sendtransaction.SendTransactionSettings
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

interface ISwapXxxProvider {
    val id: String
    val title: String
    val url: String
    val icon: Int

    fun supports(tokenFrom: Token, tokenTo: Token): Boolean {
        return tokenFrom.blockchainType == tokenTo.blockchainType &&
            supports(tokenFrom.blockchainType)
    }

    fun supports(blockchainType: BlockchainType): Boolean
    suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>
    ): ISwapQuote

    suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        swapSettings: Map<String, Any?>,
        sendTransactionSettings: SendTransactionSettings?
    ) : ISwapFinalQuote {
        TODO()
    }
}
