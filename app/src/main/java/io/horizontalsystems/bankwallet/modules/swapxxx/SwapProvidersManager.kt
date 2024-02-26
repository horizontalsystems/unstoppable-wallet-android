package io.horizontalsystems.bankwallet.modules.swapxxx

import android.util.Log
import io.horizontalsystems.bankwallet.modules.swapxxx.providers.ISwapXxxProvider
import io.horizontalsystems.bankwallet.modules.swapxxx.providers.OneInchProvider
import io.horizontalsystems.bankwallet.modules.swapxxx.providers.PancakeSwapProvider
import io.horizontalsystems.bankwallet.modules.swapxxx.providers.PancakeSwapV3Provider
import io.horizontalsystems.bankwallet.modules.swapxxx.providers.QuickSwapProvider
import io.horizontalsystems.bankwallet.modules.swapxxx.providers.UniswapProvider
import io.horizontalsystems.bankwallet.modules.swapxxx.providers.UniswapV3Provider
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

    suspend fun getQuotes(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>
    ) = coroutineScope {
        val providers = allProviders.filter {
            it.supports(tokenIn, tokenOut)
        }
        providers
            .map { provider ->
                async {
                    try {
                        val quote = provider.fetchQuote(tokenIn, tokenOut, amountIn, settings)
                        SwapProviderQuote(
                            provider = provider,
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
    val swapQuote: ISwapQuote
) {
    val tokenIn by swapQuote::tokenIn
    val tokenOut by swapQuote::tokenOut
    val amountIn by swapQuote::amountIn
    val amountOut by swapQuote::amountOut
    val fee by swapQuote::fee
    val fields by swapQuote::fields
    val priceImpact by swapQuote::priceImpact

    val createdAt = System.currentTimeMillis()
    val expireAt = createdAt + 30000L
}
