package io.horizontalsystems.bankwallet.modules.multiswap

import android.util.Log
import io.horizontalsystems.bankwallet.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.OneInchProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.PancakeSwapProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.PancakeSwapV3Provider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.QuickSwapProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.UniswapProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.UniswapV3Provider
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal

class SwapQuoteService {
    private val allProviders = listOf(
        OneInchProvider,
        PancakeSwapProvider,
        PancakeSwapV3Provider,
        QuickSwapProvider,
        UniswapProvider,
        UniswapV3Provider,
    )

    private var amountIn: BigDecimal? = null
    private var tokenIn: Token? = null
    private var tokenOut: Token? = null
    private var quoting = false
    private var quotes: List<SwapProviderQuote> = listOf()
    private var preferredProvider: IMultiSwapProvider? = null
    private var error: Throwable? = null
    private var quote: SwapProviderQuote? = null

    private val _stateFlow = MutableStateFlow(
        State(
            amountIn = amountIn,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            quoting = quoting,
            quotes = quotes,
            preferredProvider = preferredProvider,
            quote = quote,
            error = error,
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    private var coroutineScope = CoroutineScope(Dispatchers.Default)
    private var quotingJob: Job? = null
    private var settings: Map<String, Any?> = mapOf()

    private fun emitState() {
        _stateFlow.update {
            State(
                amountIn = amountIn,
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                quoting = quoting,
                quotes = quotes,
                preferredProvider = preferredProvider,
                quote = quote,
                error = error,
            )
        }
    }

    private fun runQuotation() {
        quotingJob?.cancel()
        quoting = false
        quotes = listOf()
        quote = null
        error = null

        emitState()

        val tokenIn = tokenIn
        val tokenOut = tokenOut
        val amountIn = amountIn

        if (tokenIn != null && tokenOut != null) {
            val supportedProviders = allProviders.filter { it.supports(tokenIn, tokenOut) }

            if (supportedProviders.isEmpty()) {
                error = NoSupportedSwapProvider()
                emitState()
            } else if (amountIn != null && amountIn > BigDecimal.ZERO) {
                quoting = true
                emitState()

                quotingJob = coroutineScope.launch {
                    quotes = fetchQuotes(supportedProviders, tokenIn, tokenOut, amountIn)

                    if (preferredProvider != null && quotes.none { it.provider == preferredProvider }) {
                        preferredProvider = null
                    }

                    if (quotes.isEmpty()) {
                        error = SwapRouteNotFound()
                    } else {
                        quote = preferredProvider
                            ?.let { provider -> quotes.find { it.provider == provider } }
                            ?: quotes.firstOrNull()
                    }

                    quoting = false
                    emitState()
                }
            }
        }
    }

    private suspend fun fetchQuotes(
        supportedProviders: List<IMultiSwapProvider>,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
    ) = coroutineScope {
        supportedProviders
            .map { provider ->
                async {
                    try {
                        withTimeout(5000) {
                            val quote = provider.fetchQuote(tokenIn, tokenOut, amountIn, settings)
                            SwapProviderQuote(provider = provider, swapQuote = quote)
                        }
                    } catch (e: Throwable) {
                        Log.d("AAA", "fetchQuoteError: ${provider.id}", e)
                        null
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
            .sortedByDescending { it.amountOut }
    }

    fun setAmount(v: BigDecimal?) {
        if (amountIn == v) return

        amountIn = v
        preferredProvider = null

        runQuotation()
    }

    fun setTokenIn(token: Token) {
        if (tokenIn == token) return

        tokenIn = token
        preferredProvider = null
        if (tokenOut == token) {
            tokenOut = null
        }

        runQuotation()
    }

    fun setTokenOut(token: Token) {
        if (tokenOut == token) return

        tokenOut = token
        preferredProvider = null
        if (tokenIn == token) {
            tokenIn = null
        }

        runQuotation()
    }

    fun switchPairs() {
        val tmpTokenIn = tokenIn

        tokenIn = tokenOut
        tokenOut = tmpTokenIn

        amountIn = quote?.amountOut

        runQuotation()
    }

    fun selectQuote(quote: SwapProviderQuote) {
        preferredProvider = quote.provider
        this.quote = quote

        emitState()
    }

    fun reQuote() {
        runQuotation()
    }

    fun setSwapSettings(settings: Map<String, Any?>) {
        this.settings = settings

        runQuotation()
    }

    fun onActionStarted() {
        preferredProvider = quote?.provider
    }

    fun onActionCompleted() {
        reQuote()
    }

    fun getSwapSettings() = settings

    data class State(
        val amountIn: BigDecimal?,
        val tokenIn: Token?,
        val tokenOut: Token?,
        val quoting: Boolean,
        val quotes: List<SwapProviderQuote>,
        val preferredProvider: IMultiSwapProvider?,
        val quote: SwapProviderQuote?,
        val error: Throwable?,
    )
}
