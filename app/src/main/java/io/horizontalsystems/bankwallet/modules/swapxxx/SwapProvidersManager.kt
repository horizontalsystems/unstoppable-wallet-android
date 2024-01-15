package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapQuote
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchKitHelper
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchTradeService
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapV2TradeService
import io.horizontalsystems.bankwallet.modules.swap.uniswapv3.UniswapV3TradeService
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.UniswapV3Kit
import io.horizontalsystems.uniswapkit.models.DexType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

class SwapProvidersManager {
    private val oneIncKitHelper by lazy { OneInchKitHelper(App.appConfigProvider.oneInchApiKey) }
    private val uniswapKit by lazy { UniswapKit.getInstance() }
    private val uniswapV3Kit by lazy { UniswapV3Kit.getInstance(DexType.Uniswap) }
    private val pancakeSwapV3Kit by lazy { UniswapV3Kit.getInstance(DexType.PancakeSwap) }

    private val allProviders = listOf(
        SwapMainModule.OneInchProvider,
        SwapMainModule.PancakeSwapProvider,
        SwapMainModule.PancakeSwapV3Provider,
        SwapMainModule.QuickSwapProvider,
        SwapMainModule.UniswapProvider,
        SwapMainModule.UniswapV3Provider,
    )
    private val oneInchTradeService = OneInchTradeService(oneIncKitHelper)
    private val uniswapV3TradeService = UniswapV3TradeService(uniswapV3Kit)
    private val uniswapV3TradeService1 = UniswapV3TradeService(pancakeSwapV3Kit)
    private val uniswapV2TradeService = UniswapV2TradeService(uniswapKit)

    suspend fun getQuotes(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal) = coroutineScope {
        val providers = allProviders.filter {
            it.supports(tokenIn, tokenOut)
        }
        providers
            .map { provider ->
                async {
                    try {
                        val quote = getTradeService(provider).fetchQuote(tokenIn, tokenOut, amountIn)
                        SwapProviderQuote(provider, quote)
                    } catch (e: Throwable) {
                        null
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
    }

    private fun getTradeService(provider: SwapMainModule.ISwapProvider): SwapMainModule.ISwapTradeService = when (provider) {
        SwapMainModule.OneInchProvider -> oneInchTradeService
        SwapMainModule.UniswapV3Provider -> uniswapV3TradeService
        SwapMainModule.PancakeSwapV3Provider -> uniswapV3TradeService1
        else -> uniswapV2TradeService
    }

}

data class SwapProviderQuote(
    val provider: SwapMainModule.ISwapProvider,
    val quote: SwapQuote
)
