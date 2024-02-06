package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.modules.swapxxx.providers.ISwapXxxProvider
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapQuoteService(private val swapProvidersManager: SwapProvidersManager) {
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
    private var scheduleReQuoteJob: Job? = null
    private var settings: Map<String, Any?> = mapOf()

    fun setAmount(v: BigDecimal?) {
        amountIn = v
        preferredProvider = null

        runQuotation()
    }

    fun setTokenIn(token: Token) {
        tokenIn = token
        preferredProvider = null

        runQuotation()
    }

    fun setTokenOut(token: Token) {
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
        quotingJob?.cancel()
        scheduleReQuoteJob?.cancel()

        quotes = listOf()
        quote = null
        error = null

        val amountIn = amountIn
        val tokenIn = tokenIn
        val tokenOut = tokenOut

        if (amountIn == null || amountIn <= BigDecimal.ZERO || tokenIn == null || tokenOut == null) {
            quoting = false
            emitState()
        } else {
            quoting = true
            emitState()

            quotingJob = coroutineScope.launch {
                quotes = swapProvidersManager.getQuotes(tokenIn, tokenOut, amountIn, settings).sortedByDescending { it.amountOut }
                if (preferredProvider != null && quotes.none { it.provider == preferredProvider}) {
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

                scheduleReQuote()
            }
        }
    }

    private fun scheduleReQuote() {
        scheduleReQuoteJob = coroutineScope.launch {
            delay(quoteLifetime)
            runQuotation()
        }
    }

    fun setSwapSettings(settings: Map<String, Any?>) {
        this.settings = settings
        runQuotation()
    }

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
