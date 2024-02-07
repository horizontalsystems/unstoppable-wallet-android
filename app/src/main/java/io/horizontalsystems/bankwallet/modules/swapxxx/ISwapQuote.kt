package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.ISwapSetting
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapDataField
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
