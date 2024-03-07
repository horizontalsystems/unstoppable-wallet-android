package cash.p.terminal.modules.swapxxx

import android.util.Log
import cash.p.terminal.modules.swapxxx.providers.ISwapXxxProvider
import cash.p.terminal.modules.swapxxx.providers.OneInchProvider
import cash.p.terminal.modules.swapxxx.providers.PancakeSwapProvider
import cash.p.terminal.modules.swapxxx.providers.PancakeSwapV3Provider
import cash.p.terminal.modules.swapxxx.providers.QuickSwapProvider
import cash.p.terminal.modules.swapxxx.providers.UniswapProvider
import cash.p.terminal.modules.swapxxx.providers.UniswapV3Provider
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private val quoteLifetime = 30000L
    private var amountIn: BigDecimal? = null
    private var tokenIn: Token? = null
    private var tokenOut: Token? = null
    private var quoting = false
    private var quotes: List<SwapProviderQuote> = listOf()
    private var preferredProvider: ISwapXxxProvider? = null
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
            quoteLifetime = quoteLifetime,
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
                quoteLifetime = quoteLifetime,
                quote = quote,
                error = error,
            )
        }
    }

    private fun runQuotation() {
        cancelCurrentQuotation()
        resetOutput()

        emitState()

        fetchQuotes()
    }

    private fun cancelCurrentQuotation() {
        quotingJob?.cancel()
        quoting = false
    }

    private fun resetOutput() {
        quotes = listOf()
        quote = null
        error = null
    }

    private fun fetchQuotes() {
        val amountIn = amountIn
        val tokenIn = tokenIn
        val tokenOut = tokenOut

        if (tokenIn != null && tokenOut != null) {
            val supportedProviders = allProviders.filter { it.supports(tokenIn, tokenOut) }

            if (supportedProviders.isEmpty()) {
                error = NoSupportedSwapProvider()
                emitState()
            } else if (amountIn != null && amountIn > BigDecimal.ZERO) {
                quoting = true
                emitState()

                quotingJob = coroutineScope.launch {
                    quotes = supportedProviders
                        .map { provider ->
                            async {
                                try {
                                    val quote =
                                        provider.fetchQuote(tokenIn, tokenOut, amountIn, settings)
                                    SwapProviderQuote(provider = provider, swapQuote = quote)
                                } catch (e: Throwable) {
                                    Log.e("AAA", "fetchQuoteError: ${provider.id}", e)
                                    null
                                }
                            }
                        }
                        .awaitAll()
                        .filterNotNull()
                        .sortedByDescending { it.amountOut }

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

        runQuotation()
    }

    fun setTokenOut(token: Token) {
        if (tokenOut == token) return

        tokenOut = token
        preferredProvider = null

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

    fun getSwapSettings() = settings

    data class State(
        val amountIn: BigDecimal?,
        val tokenIn: Token?,
        val tokenOut: Token?,
        val quoting: Boolean,
        val quotes: List<SwapProviderQuote>,
        val preferredProvider: ISwapXxxProvider?,
        val quoteLifetime: Long,
        val quote: SwapProviderQuote?,
        val error: Throwable?,
    )
}
