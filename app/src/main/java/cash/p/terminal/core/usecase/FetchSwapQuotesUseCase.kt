package cash.p.terminal.core.usecase

import cash.p.terminal.modules.multiswap.SwapProviderQuote
import cash.p.terminal.modules.multiswap.sortedByBestAmountOut
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.wallet.Token
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException

class FetchSwapQuotesUseCase {

    suspend operator fun invoke(
        providers: List<IMultiSwapProvider>,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?> = emptyMap(),
        onProviderError: ((IMultiSwapProvider, Throwable) -> Unit)? = null,
    ): List<SwapProviderQuote> = coroutineScope {
        val supported = filterSupported(providers, tokenIn, tokenOut)
        if (supported.isEmpty()) return@coroutineScope emptyList()

        fetchQuotes(supported, tokenIn, tokenOut, amountIn, settings, onProviderError)
            .sortedByBestAmountOut()
    }

    private suspend fun filterSupported(
        providers: List<IMultiSwapProvider>,
        tokenIn: Token,
        tokenOut: Token,
    ) = coroutineScope {
        providers.map { provider ->
            async {
                try {
                    withTimeoutOrNull(TIMEOUT_MS) {
                        if (provider.supports(tokenIn, tokenOut)) provider else null
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Timber.d(e, "supports error: ${provider.id}")
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun fetchQuotes(
        providers: List<IMultiSwapProvider>,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>,
        onProviderError: ((IMultiSwapProvider, Throwable) -> Unit)?,
    ) = coroutineScope {
        providers.map { provider ->
            async {
                try {
                    withTimeoutOrNull(TIMEOUT_MS) {
                        val quote = provider.fetchQuote(tokenIn, tokenOut, amountIn, settings)
                        SwapProviderQuote(provider = provider, swapQuote = quote)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    onProviderError?.invoke(provider, e)
                        ?: Timber.d(e, "fetchQuoteError: ${provider.id}")
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
    }
}
