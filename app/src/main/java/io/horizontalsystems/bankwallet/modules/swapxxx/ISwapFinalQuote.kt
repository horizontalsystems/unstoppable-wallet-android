package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.modules.swapxxx.sendtransaction.SendTransactionData
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

interface ISwapFinalQuote {
    val tokenIn: Token
    val tokenOut: Token
    val amountIn: BigDecimal
    val amountOut: BigDecimal
    val sendTransactionData: SendTransactionData
}

data class SwapFinalQuoteUniswapV3(
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    override val amountOut: BigDecimal,
    override val sendTransactionData: SendTransactionData.Evm,
) : ISwapFinalQuote

data class SwapFinalQuoteOneInch(
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    override val amountOut: BigDecimal,
    override val sendTransactionData: SendTransactionData.Evm,
) : ISwapFinalQuote
