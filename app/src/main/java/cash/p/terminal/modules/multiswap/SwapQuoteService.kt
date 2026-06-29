package cash.p.terminal.modules.multiswap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cash.p.terminal.core.usecase.FetchSwapQuotesUseCase
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.modules.multiswap.providers.SwapProvidersRegistry
import cash.p.terminal.modules.multiswap.providers.SwapProvidersRepository
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal

class SwapQuoteService(
    private val routeResolver: MultiSwapRouteResolver,
    private val fetchSwapQuotesUseCase: FetchSwapQuotesUseCase,
    private val swapProvidersRepository: SwapProvidersRepository,
    private val swapProvidersRegistry: SwapProvidersRegistry,
    private val dispatcherProvider: DispatcherProvider,
) {

    private companion object {
        const val DEBOUNCE_INPUT_MSEC: Long = 300
    }

    private var runQuotationJob: Job? = null

    private val allProviders: List<IMultiSwapProvider>
        get() = swapProvidersRegistry.providers

    val providers: List<IMultiSwapProvider> get() = allProviders

    private val enabledProviders: List<IMultiSwapProvider>
        get() = allProviders.filterNot { swapProvidersRepository.isDisabled(it.id) }

    private val disabledByUserProviders: List<IMultiSwapProvider>
        get() = allProviders.filter { swapProvidersRepository.isDisabled(it.id) }

    fun findProviderById(id: String): IMultiSwapProvider? =
        swapProvidersRegistry.findById(id)

    private var amountIn: BigDecimal? = null
    private var tokenIn: Token? = null
    private var tokenOut: Token? = null
    private var quoting = false
    private var quotes: List<SwapProviderQuote> = listOf()
    private var preferredProvider: IMultiSwapProvider? = null
    private var error by mutableStateOf<Throwable?>(null)
    private var quote: SwapProviderQuote? = null
    private var multiSwapRoute: MultiSwapRoute? = null

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
            multiSwapRoute = multiSwapRoute,
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    private val coroutineScope = CoroutineScope(dispatcherProvider.io + SupervisorJob())
    private var quotingJob: Job? = null
    private var settings: Map<String, Any?> = mapOf()

    fun clear() {
        coroutineScope.cancel()
    }

    init {
        coroutineScope.launch {
            swapProvidersRepository.disabledIds
                .drop(1)
                .collect {
                    onDisabledProvidersChanged()
                }
        }
    }

    private fun onDisabledProvidersChanged() {
        val enabledQuotes = quotes.filterNot {
            swapProvidersRepository.isDisabled(it.provider.id)
        }
        val currentProviderId = quote?.provider?.id
        val currentStillEnabled = enabledQuotes.any { it.provider.id == currentProviderId }

        when {
            currentStillEnabled -> Unit
            enabledQuotes.isNotEmpty() -> {
                quote = enabledQuotes.first()
                error = null
                multiSwapRoute = null
                emitState()
            }
            else -> runQuotation()
        }
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
                multiSwapRoute = multiSwapRoute,
            )
        }
    }

    suspend fun start() = withContext(dispatcherProvider.io) {
        allProviders.forEach {
            try {
                it.start()
            } catch (e: Throwable) {
                Timber.d(e, "error on starting ${it.id}")
            }
        }
    }

    private fun runQuotation(clearQuotes: Boolean = false) {
        quotingJob?.cancel()
        quoting = false
        if (clearQuotes) {
            quotes = listOf()
            quote = null
            multiSwapRoute = null
        }
        error = null

        if (clearQuotes) {
            emitState()
        }

        val tokenIn = tokenIn
        val tokenOut = tokenOut
        val amountIn = amountIn

        if (tokenIn != null && tokenOut != null) {
            quotingJob = coroutineScope.launch {
                if (amountIn != null && amountIn > BigDecimal.ZERO) {
                    quoting = true
                    emitState()

                    val newQuotes = fetchSwapQuotesUseCase(
                        providers = allProviders,
                        tokenIn = tokenIn,
                        tokenOut = tokenOut,
                        amountIn = amountIn,
                        settings = settings,
                        onProviderError = { _, e ->
                            if (e is SwapDepositTooSmall) {
                                val current = error as? SwapDepositTooSmall
                                if (current == null || current.minValue > e.minValue) {
                                    error = e
                                }
                            } else {
                                Timber.d(e, "fetchQuoteError")
                            }
                        },
                    )
                    if (amountIn != this@SwapQuoteService.amountIn) {
                        return@launch // ignore outdated quotes
                    }
                    quotes = newQuotes

                    if (preferredProvider != null && quotes.none { it.provider == preferredProvider }) {
                        preferredProvider = null
                    }

                    val enabledQuotes = newQuotes.filterNot {
                        swapProvidersRepository.isDisabled(it.provider.id)
                    }

                    if (enabledQuotes.isEmpty()) {
                        tryFallbackToMultiSwapRoute(tokenIn, tokenOut, amountIn, noDirectProviders = enabledQuotes.isEmpty())
                        quote = multiSwapRoute?.leg1Quotes?.firstOrNull()
                    } else {
                        error = null
                        multiSwapRoute = null
                        quote = preferredProvider
                            ?.let { provider -> enabledQuotes.find { it.provider == provider } }
                            ?: enabledQuotes.firstOrNull()
                    }

                    quoting = false
                    emitState()
                } else {
                    // Amount is null or zero - clear quotes, don't set error yet
                    quotes = listOf()
                    quote = null
                    multiSwapRoute = null
                    emitState()
                }
            }
        } else {
            // Tokens are null - clear quotes
            quotes = listOf()
            quote = null
            multiSwapRoute = null
            emitState()
        }
    }

    private suspend fun tryFallbackToMultiSwapRoute(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        noDirectProviders: Boolean,
    ) {
        val route = routeResolver.findRoute(enabledProviders, tokenIn, tokenOut, amountIn, settings)
        if (route != null) {
            multiSwapRoute = route
            error = null
        } else {
            multiSwapRoute = null
            error = resolveEmptyResultError(tokenIn, tokenOut, noDirectProviders)
        }
    }

    private suspend fun resolveEmptyResultError(
        tokenIn: Token,
        tokenOut: Token,
        noDirectProviders: Boolean,
    ): Throwable = when {
        !noDirectProviders -> SwapRouteNotFound()
        disabledByUserProviders.isEmpty() -> NoSupportedSwapProvider()
        fetchSwapQuotesUseCase
            .findSupportedProviders(disabledByUserProviders, tokenIn, tokenOut)
            .isNotEmpty() -> NoEnabledSwapProvider()
        else -> NoSupportedSwapProvider()
    }


    fun setAmount(v: BigDecimal?) {
        if (amountIn == v) {
            runQuotationWithDebounce()
            return
        }

        amountIn = v
        preferredProvider = null

        runQuotationWithDebounce()
    }

    private fun runQuotationWithDebounce() {
        runQuotationJob?.cancel()

        quotingJob?.cancel()
        quoting = false
        // Keep previous quotes during requoting to prevent choose provider from closing
        error = null
        emitState()

        runQuotationJob = coroutineScope.launch {
            delay(DEBOUNCE_INPUT_MSEC)
            runQuotation()
        }
    }

    fun setTokenIn(token: Token) {
        if (tokenIn == token) return

        tokenIn = token
        preferredProvider = null
        if (tokenOut == token) {
            tokenOut = null
        }

        runQuotation(clearQuotes = true)
    }

    fun setTokenOut(token: Token) {
        if (tokenOut == token) return

        tokenOut = token
        preferredProvider = null
        if (tokenIn == token) {
            tokenIn = null
        }

        runQuotation(clearQuotes = true)
    }

    fun switchPairs() {
        val tmpTokenIn = tokenIn

        tokenIn = tokenOut
        tokenOut = tmpTokenIn

        amountIn = multiSwapRoute?.selectedLeg2Quote?.amountOut ?: quote?.amountOut

        runQuotation(clearQuotes = true)
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
        val multiSwapRoute: MultiSwapRoute?,
    )
}
