package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.modules.multiswap.providers.IMultiSwapProvider

data class SwapProviderQuote(
    val provider: IMultiSwapProvider,
    val swapQuote: SwapQuote,
    val createdAt: Long = System.currentTimeMillis()
) {
    val tokenIn by swapQuote::tokenIn
    val tokenOut by swapQuote::tokenOut
    val amountIn by swapQuote::amountIn
    val amountOut by swapQuote::amountOut
    val fields by swapQuote::fields
    val actionRequired by swapQuote::actionRequired
}
