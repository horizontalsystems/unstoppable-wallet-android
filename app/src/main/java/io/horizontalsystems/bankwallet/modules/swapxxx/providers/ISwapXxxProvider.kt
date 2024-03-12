package cash.p.terminal.modules.swapxxx.providers

import cash.p.terminal.modules.swapxxx.ISwapQuote
import cash.p.terminal.modules.swapxxx.sendtransaction.SendTransactionData
import cash.p.terminal.modules.swapxxx.sendtransaction.SendTransactionSettings
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

    suspend fun swap(swapQuote: ISwapQuote) {

    }

    suspend fun getSendTransactionData(
        swapQuote: ISwapQuote,
        sendTransactionSettings: SendTransactionSettings?,
        swapSettings: Map<String, Any?>
    ): SendTransactionData = TODO()
}
