package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.ISwapSettingField
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapDataField
import java.math.BigDecimal

interface ISwapQuote {
    val amountOut: BigDecimal
    val fields: List<SwapDataField>
    val fee: SendModule.AmountData?
    val settingFields: List<ISwapSettingField>
}

class SwapQuoteUniswap(
    override val amountOut: BigDecimal,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    override val settingFields: List<ISwapSettingField>,
) : ISwapQuote

class SwapQuoteUniswapV3(
    override val amountOut: BigDecimal,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    override val settingFields: List<ISwapSettingField>,
) : ISwapQuote

class SwapQuoteOneInch(
    override val amountOut: BigDecimal,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    override val settingFields: List<ISwapSettingField>,
) : ISwapQuote
