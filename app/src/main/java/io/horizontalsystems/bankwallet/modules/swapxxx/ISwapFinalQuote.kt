package cash.p.terminal.modules.swapxxx

import cash.p.terminal.modules.swapxxx.sendtransaction.SendTransactionData
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

interface ISwapFinalQuote {
    val tokenIn: Token
    val tokenOut: Token
    val amountIn: BigDecimal
    val amountOut: BigDecimal
    val amountOutMin: BigDecimal
    val sendTransactionData: SendTransactionData
    val priceImpact: BigDecimal?
}

data class SwapFinalQuoteUniswapV3(
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    override val amountOut: BigDecimal,
    override val amountOutMin: BigDecimal,
    override val sendTransactionData: SendTransactionData.Evm,
    override val priceImpact: BigDecimal?
) : ISwapFinalQuote

data class SwapFinalQuoteOneInch(
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    override val amountOut: BigDecimal,
    override val amountOutMin: BigDecimal,
    override val sendTransactionData: SendTransactionData.Evm,
    override val priceImpact: BigDecimal?
) : ISwapFinalQuote
