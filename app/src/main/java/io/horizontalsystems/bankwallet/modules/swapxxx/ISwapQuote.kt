package cash.p.terminal.modules.swapxxx

import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.modules.swapxxx.settings.ISwapSetting
import cash.p.terminal.modules.swapxxx.ui.SwapDataField
import java.math.BigDecimal

interface ISwapQuote {
    val amountOut: BigDecimal
    val priceImpact: BigDecimal?
    val fields: List<SwapDataField>
    val fee: SendModule.AmountData?
    val settings: List<ISwapSetting>
}

class SwapQuoteUniswap(
    override val amountOut: BigDecimal,
    override val priceImpact: BigDecimal?,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    override val settings: List<ISwapSetting>,
) : ISwapQuote

class SwapQuoteUniswapV3(
    override val amountOut: BigDecimal,
    override val priceImpact: BigDecimal?,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    override val settings: List<ISwapSetting>,
) : ISwapQuote

class SwapQuoteOneInch(
    override val amountOut: BigDecimal,
    override val priceImpact: BigDecimal?,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    override val settings: List<ISwapSetting>,
) : ISwapQuote
