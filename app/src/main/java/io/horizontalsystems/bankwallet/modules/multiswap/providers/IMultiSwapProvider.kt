package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.modules.multiswap.ISwapFinalQuote
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapQuote
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
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
