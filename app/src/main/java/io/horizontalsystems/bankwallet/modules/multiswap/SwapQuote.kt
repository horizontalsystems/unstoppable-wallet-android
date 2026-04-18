package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

data class SwapQuote(
    val amountOut: BigDecimal,
    val tokenIn: Token,
    val tokenOut: Token,
    val amountIn: BigDecimal,
    val actionRequired: ISwapProviderAction?,
    val estimationTime: Long?,
)
