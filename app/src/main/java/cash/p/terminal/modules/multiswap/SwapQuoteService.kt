package cash.p.terminal.modules.multiswap

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cash.p.terminal.core.usecase.FetchSwapQuotesUseCase
import cash.p.terminal.modules.multiswap.providers.AllBridgeProvider
import cash.p.terminal.modules.multiswap.providers.ChangeNowProvider
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.modules.multiswap.providers.MayaProvider
import cash.p.terminal.modules.multiswap.providers.OneInchProvider
import cash.p.terminal.modules.multiswap.providers.PancakeSwapProvider
import cash.p.terminal.modules.multiswap.providers.PancakeSwapV3Provider
import cash.p.terminal.modules.multiswap.providers.QuickSwapProvider
import cash.p.terminal.modules.multiswap.providers.QuickexProvider
import cash.p.terminal.modules.multiswap.providers.StonFiProvider
import cash.p.terminal.modules.multiswap.providers.ThorChainProvider
import cash.p.terminal.modules.multiswap.providers.UniswapProvider
import cash.p.terminal.modules.multiswap.providers.UniswapV3Provider
import cash.p.terminal.wallet.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.math.BigDecimal

class SwapQuoteService(
    changeNowProvider: ChangeNowProvider,
    quickexProvider: QuickexProvider,
    private val routeResolver: MultiSwapRouteResolver,
    private val fetchSwapQuotesUseCase: FetchSwapQuotesUseCase,
) {
    private val stonFiProvider: StonFiProvider by inject(StonFiProvider::class.java)

    private companion object {
        const val DEBOUNCE_INPUT_MSEC: Long = 300
    }

    private var runQuotationJob: Job? = null

    @VisibleForTesting
    internal var allProviders: List<IMultiSwapProvider> = listOf(
        OneInchProvider,
        PancakeSwapProvider,
        PancakeSwapV3Provider,
        QuickSwapProvider,
        UniswapProvider,
        UniswapV3Provider,
        changeNowProvider,
        quickexProvider,
        ThorChainProvider,
        MayaProvider,
        AllBridgeProvider,
        stonFiProvider
    )

    val providers: List<IMultiSwapProvider> get() = allProviders

    fun findProviderById(id: String): IMultiSwapProvider? =
        allProviders.firstOrNull { it.id == id }

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

    @VisibleForTesting
    internal var coroutineScope = CoroutineScope(Dispatchers.IO)
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
                multiSwapRoute = multiSwapRoute,
            )
        }
    }

    suspend fun start() = withContext(Dispatchers.IO) {
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

                    if (quotes.isEmpty()) {
                        tryFallbackToMultiSwapRoute(tokenIn, tokenOut, amountIn, noDirectProviders = newQuotes.isEmpty())
                        quote = multiSwapRoute?.leg1Quotes?.firstOrNull()
                    } else {
                        error = null
                        multiSwapRoute = null
                        quote = preferredProvider
                            ?.let { provider -> quotes.find { it.provider == provider } }
                            ?: quotes.firstOrNull()
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
        val route = routeResolver.findRoute(allProviders, tokenIn, tokenOut, amountIn, settings)
        if (route != null) {
            multiSwapRoute = route
            error = null
        } else {
            multiSwapRoute = null
            error = if (noDirectProviders) NoSupportedSwapProvider() else SwapRouteNotFound()
        }
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

    fun selectLeg1Quote(quote: SwapProviderQuote) {
        val route = multiSwapRoute ?: return
        val leg2Amount = quote.amountOut - route.commissionReserve
        if (leg2Amount <= BigDecimal.ZERO) return

        multiSwapRoute = route.copy(selectedLeg1Quote = quote)
        emitState()

        reQuoteLeg2(leg2Amount)
    }

    fun selectLeg2Quote(quote: SwapProviderQuote) {
        val route = multiSwapRoute ?: return
        multiSwapRoute = route.copy(selectedLeg2Quote = quote)
        emitState()
    }

    private fun reQuoteLeg2(leg2Amount: BigDecimal) {
        val route = multiSwapRoute ?: return
        val tokenOut = tokenOut ?: return

        coroutineScope.launch {
            val leg2Providers = route.leg2Quotes.map { it.provider }
            val newLeg2Quotes = fetchSwapQuotesUseCase(
                providers = leg2Providers,
                tokenIn = route.intermediateCoin,
                tokenOut = tokenOut,
                amountIn = leg2Amount,
                settings = settings,
            )
            if (newLeg2Quotes.isEmpty()) return@launch

            multiSwapRoute = multiSwapRoute?.copy(
                leg2Quotes = newLeg2Quotes,
                selectedLeg2Quote = newLeg2Quotes.first(),
            )
            emitState()
        }
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
