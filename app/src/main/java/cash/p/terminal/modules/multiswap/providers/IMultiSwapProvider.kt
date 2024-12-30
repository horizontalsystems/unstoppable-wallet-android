package cash.p.terminal.modules.multiswap.providers

import cash.p.terminal.modules.multiswap.ISwapFinalQuote
import cash.p.terminal.modules.multiswap.ISwapQuote
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.entities.BlockchainType
import java.math.BigDecimal

interface IMultiSwapProvider {
    val id: String
    val title: String
    val url: String
    val icon: Int
    val priority: Int

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
    ) : ISwapFinalQuote
}
