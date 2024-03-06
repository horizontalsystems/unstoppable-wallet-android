package cash.p.terminal.modules.swapxxx

import cash.p.terminal.modules.swapxxx.providers.ISwapXxxProvider

data class SwapProviderQuote(
    val provider: ISwapXxxProvider,
    val swapQuote: ISwapQuote
) {
    val tokenIn by swapQuote::tokenIn
    val tokenOut by swapQuote::tokenOut
    val amountIn by swapQuote::amountIn
    val amountOut by swapQuote::amountOut
    val fields by swapQuote::fields
    val priceImpact by swapQuote::priceImpact

    val createdAt = System.currentTimeMillis()
    val expireAt = createdAt + 30000L
}
