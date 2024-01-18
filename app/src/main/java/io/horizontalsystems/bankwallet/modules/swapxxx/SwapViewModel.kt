package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapViewModel(
    private val swapProvidersManager: SwapProvidersManager,
    private val adapterManager: IAdapterManager
) : ViewModel() {
    private val quoteLifetime = 30000L
    private var amountIn: BigDecimal? = null
    private var tokenIn: Token? = null
    private var tokenOut: Token? = null
    private var quoting = false
    private var quotes: List<SwapProviderQuote> = listOf()
    private var preferredProvider: SwapMainModule.ISwapProvider? = null
    private var error: Throwable? = null
    private var quote: SwapProviderQuote? = null
    private var availableBalance: BigDecimal? = null

    var uiState: SwapUiState by mutableStateOf(
        SwapUiState(
            amountIn = amountIn,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            quoting = quoting,
            swapEnabled = isSwapEnabled(),
            quotes = quotes,
            preferredProvider = preferredProvider,
            quoteLifetime = quoteLifetime,
            quote = quote,
            error = error,
            availableBalance = availableBalance
        )
    )
        private set

    private var quotingJob: Job? = null

    fun onEnterAmount(v: BigDecimal?) {
        amountIn = v
        preferredProvider = null

        runQuotation()
    }

    fun onSelectTokenIn(token: Token) {
        tokenIn = token
        refreshAvailableBalance()
        preferredProvider = null

        runQuotation()
    }

    fun onSelectTokenOut(token: Token) {
        tokenOut = token
        preferredProvider = null

        runQuotation()
    }

    fun onSwitchPairs() {
        val tmpTokenIn = tokenIn

        tokenIn = tokenOut
        refreshAvailableBalance()
        tokenOut = tmpTokenIn

        amountIn = quote?.quote?.amountOut

        runQuotation()
    }

    fun onSelectQuote(quote: SwapProviderQuote) {
        preferredProvider = quote.provider
        this.quote = quote

        emitState()
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = SwapUiState(
                amountIn = amountIn,
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                quoting = quoting,
                swapEnabled = isSwapEnabled(),
                quotes = quotes,
                preferredProvider = preferredProvider,
                quoteLifetime = quoteLifetime,
                quote = quote,
                error = error,
                availableBalance = availableBalance
            )
        }
    }

    private fun isSwapEnabled() = quote != null

    private fun refreshAvailableBalance() {
        availableBalance = tokenIn?.let {
            (adapterManager.getAdapterForToken(it) as? IBalanceAdapter)?.balanceData?.available
        }
    }

    private fun runQuotation() {
        quotingJob?.cancel()

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

            quotingJob = viewModelScope.launch(Dispatchers.Default) {
                quotes = swapProvidersManager.getQuotes(tokenIn, tokenOut, amountIn).sortedByDescending { it.quote.amountOut }
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
        viewModelScope.launch {
            delay(quoteLifetime)
            runQuotation()
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapViewModel(SwapProvidersManager(), App.adapterManager) as T
        }
    }
}

data class SwapUiState(
    val amountIn: BigDecimal?,
    val tokenIn: Token?,
    val tokenOut: Token?,
    val quoting: Boolean,
    val swapEnabled: Boolean,
    val quotes: List<SwapProviderQuote>,
    val preferredProvider: SwapMainModule.ISwapProvider?,
    val quoteLifetime: Long,
    val quote: SwapProviderQuote?,
    val error: Throwable?,
    val availableBalance: BigDecimal?
)
