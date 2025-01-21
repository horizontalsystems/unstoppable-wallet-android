package cash.p.terminal.modules.multiswap

import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.ui.DataField
import cash.p.terminal.wallet.Token
import java.math.BigDecimal

interface ISwapFinalQuote {
    val tokenIn: Token
    val tokenOut: Token
    val amountIn: BigDecimal
    val amountOut: BigDecimal
    val amountOutMin: BigDecimal
    val sendTransactionData: SendTransactionData
    val priceImpact: BigDecimal?
    val fields: List<DataField>
}

data class SwapFinalQuoteEvm(
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    override val amountOut: BigDecimal,
    override val amountOutMin: BigDecimal,
    override val sendTransactionData: SendTransactionData,
    override val priceImpact: BigDecimal?,
    override val fields: List<DataField>
) : ISwapFinalQuote
