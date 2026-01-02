package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.bankwallet.modules.multiswap.settings.ISwapSetting
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

data class SwapQuote(
    val amountOut: BigDecimal,
    val priceImpact: BigDecimal?,
    val fields: List<DataField>,
    val settings: List<ISwapSetting>,
    val tokenIn: Token,
    val tokenOut: Token,
    val amountIn: BigDecimal,
    val actionRequired: ISwapProviderAction?,
)
