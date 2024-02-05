package cash.p.terminal.modules.swapxxx

import android.util.Log
import cash.p.terminal.modules.swap.ISwapQuote
import cash.p.terminal.modules.swapxxx.providers.ISwapXxxProvider
import cash.p.terminal.modules.swapxxx.providers.OneInchProvider
import cash.p.terminal.modules.swapxxx.providers.PancakeSwapProvider
import cash.p.terminal.modules.swapxxx.providers.PancakeSwapV3Provider
import cash.p.terminal.modules.swapxxx.providers.QuickSwapProvider
import cash.p.terminal.modules.swapxxx.providers.UniswapProvider
import cash.p.terminal.modules.swapxxx.providers.UniswapV3Provider
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

class SwapProvidersManager {
    private val allProviders = listOf(
        OneInchProvider,
        PancakeSwapProvider,
        PancakeSwapV3Provider,
        QuickSwapProvider,
        UniswapProvider,
        UniswapV3Provider,
    )

    suspend fun getQuotes(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal) = coroutineScope {
        val providers = allProviders.filter {
            it.supports(tokenIn, tokenOut)
        }
        providers
            .map { provider ->
                async {
                    try {
                        val quote = provider.fetchQuote(tokenIn, tokenOut, amountIn)
                        SwapProviderQuote(
                            provider = provider,
                            tokenIn = tokenIn,
                            tokenOut = tokenOut,
                            amountIn = amountIn,
                            swapQuote = quote
                        )
                    } catch (e: Throwable) {
                        Log.e("AAA", "fetchQuoteError: ${provider.id}", e)
                        null
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
    }
}

data class SwapProviderQuote(
    val provider: ISwapXxxProvider,
    val tokenIn: Token,
    val tokenOut: Token,
    val amountIn: BigDecimal,
    val swapQuote: ISwapQuote
) {
    val amountOut by swapQuote::amountOut
    val fee by swapQuote::fee
    val fields by swapQuote::fields
}
