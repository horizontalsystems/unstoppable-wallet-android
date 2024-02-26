package cash.p.terminal.modules.swapxxx

import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.modules.swapxxx.settings.ISwapSetting
import cash.p.terminal.modules.swapxxx.ui.SwapDataField
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.uniswapkit.v3.TradeDataV3
import java.math.BigDecimal

interface ISwapQuote {
    val amountOut: BigDecimal
    val priceImpact: BigDecimal?
    val fields: List<SwapDataField>
    val fee: SendModule.AmountData?
    val settings: List<ISwapSetting>
    val tokenIn: Token
    val tokenOut: Token
    val amountIn: BigDecimal
}

class SwapQuoteUniswap(
    override val amountOut: BigDecimal,
    override val priceImpact: BigDecimal?,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    override val settings: List<ISwapSetting>,
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
) : ISwapQuote

class SwapQuoteUniswapV3(
    val tradeDataV3: TradeDataV3,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    override val settings: List<ISwapSetting>,
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    val transactionData: TransactionData?,
) : ISwapQuote {
    override val amountOut = tradeDataV3.tokenAmountOut.decimalAmount!!
    override val priceImpact = tradeDataV3.priceImpact
}

class SwapQuoteOneInch(
    override val amountOut: BigDecimal,
    override val priceImpact: BigDecimal?,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    override val settings: List<ISwapSetting>,
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
) : ISwapQuote
