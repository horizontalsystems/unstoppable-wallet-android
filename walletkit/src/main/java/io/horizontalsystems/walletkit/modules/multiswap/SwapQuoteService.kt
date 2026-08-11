package io.horizontalsystems.walletkit.modules.multiswap

import android.util.Log
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.MultiSwapProviderRegistry
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapProviderInfoManager
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal

class SwapQuoteService(
    // Callers with restricted execution capabilities (e.g. smart accounts that
    // can only broadcast a subset of providers) pass their own set.
    private val allProviders: List<IMultiSwapProvider> = MultiSwapProviderRegistry.allProviders,
    private val providerInfoManager: SwapProviderInfoManager = App.swapProviderInfoManager,
) : Clearable {
    private val tag = "SwapQuoteService"

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
    private var startProvidersJob: Job? = null

    fun start() {
        coroutineScope.launch {
            startProviders()
            runQuotation()
        }

        coroutineScope.launch {
            providerInfoManager.sync()
        }

        coroutineScope.launch {
            // Dropping the current value: quotation already reads it, and re-quoting on it would
            // cancel the quotation the launch above has just started. A sync that only changes
            // which PAIRS a provider may serve leaves the token selection untouched, so this is
            // the only thing that re-runs quotation for it.
            providerInfoManager.suspensionsFlow.drop(1).collect {
                runQuotation(silent = true)
            }
        }
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    fun restart(onRestart: () -> Unit) {
        startProvidersJob?.cancel()
        startProvidersJob = coroutineScope.launch {
            startProviders()
            onRestart()
        }
    }

    private suspend fun CoroutineScope.startProviders() {
        allProviders.map { provider ->
            async {
                try {
                    provider.start()
                } catch (e: Throwable) {
                    Log.e(tag, "error on starting ${provider.id}", e)
                }
            }
        }.awaitAll()
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
                quote = quote,
                error = error,
            )
        }
    }

    private fun runQuotation(silent: Boolean = false) {
        quotingJob?.cancel()
        quoting = false
        quotes = listOf()
        quote = null
        error = null

        if (!silent) {
            emitState()
        }

        val tokenIn = tokenIn
        val tokenOut = tokenOut
        val amountIn = amountIn

        if (tokenIn != null && tokenOut != null) {
            // Read once, here: quotation runs from both the main thread and the sync collector, so
            // the flow's own atomic read is what keeps the two from seeing different indexes.
            val suspensions = providerInfoManager.suspensionsFlow.value

            val supportedProviders = allProviders.filter { provider ->
                // Checked here, in the one place every provider passes through, rather than inside
                // each `supports` implementation — and it is the only enforcement that exists for
                // the providers this app quotes natively, since those never reach the server.
                !suspensions.isSuspended(provider.id, tokenIn, tokenOut) &&
                    provider.supports(tokenIn, tokenOut)
            }

            if (supportedProviders.isEmpty()) {
                error = NoSupportedSwapProvider()
                emitState()
            } else if (amountIn != null && amountIn > BigDecimal.ZERO) {
                if (!silent) {
                    quoting = true
                    emitState()
                }

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
                            val quote = provider.fetchQuote(tokenIn, tokenOut, amountIn)
                            SwapProviderQuote(provider = provider, swapQuote = quote)
                        }
                    } catch (e: Throwable) {
                        Log.e(tag, "fetchQuoteError: ${provider.id}", e)
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

        preferredProvider = null
        if (tokenOut == token) {
            // selected token is already on the other side, swap places instead of clearing it
            tokenOut = tokenIn
        }
        tokenIn = token

        runQuotation()
    }

    fun setTokenOut(token: Token) {
        if (tokenOut == token) return

        preferredProvider = null
        if (tokenIn == token) {
            // selected token is already on the other side, swap places instead of clearing it
            tokenIn = tokenOut
        }
        tokenOut = token

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
        runQuotation(silent = true)
    }

    fun onActionStarted(quote: SwapProviderQuote?) {
        preferredProvider = quote?.provider
    }

    fun onActionCompleted() {
        runQuotation()
    }

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
