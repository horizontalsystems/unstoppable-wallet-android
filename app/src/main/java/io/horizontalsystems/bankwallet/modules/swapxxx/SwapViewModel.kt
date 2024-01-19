package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapViewModel(
    private val swapQuoteService: SwapQuoteService
) : ViewModel() {

    private var swapQuoteState = swapQuoteService.stateFlow.value

    var uiState: SwapUiState by mutableStateOf(
        SwapUiState(
            amountIn = swapQuoteState.amountIn,
            tokenIn = swapQuoteState.tokenIn,
            tokenOut = swapQuoteState.tokenOut,
            quoting = swapQuoteState.quoting,
            swapEnabled = isSwapEnabled(),
            quotes = swapQuoteState.quotes,
            preferredProvider = swapQuoteState.preferredProvider,
            quoteLifetime = swapQuoteState.quoteLifetime,
            quote = swapQuoteState.quote,
            error = swapQuoteState.error,
            availableBalance = swapQuoteState.availableBalance
        )
    )
        private set

    private var quotingJob: Job? = null
    private var scheduleReQuoteJob: Job? = null

    init {
        viewModelScope.launch {
            swapQuoteService.stateFlow.collect {
                handleUpdatedSwapQuoteState(it)
            }
        }
    }

    private fun handleUpdatedSwapQuoteState(swapQuoteState: SwapQuoteService.State) {
        this.swapQuoteState = swapQuoteState

        emitState()
    }

    fun onEnterAmount(v: BigDecimal?) {
        swapQuoteService.setAmount(v)
    }

    fun onSelectTokenIn(token: Token) {
        swapQuoteService.setTokenIn(token)
    }

    fun onSelectTokenOut(token: Token) {
        swapQuoteService.setTokenOut(token)
    }

    fun onSwitchPairs() {
        swapQuoteService.switchPairs()
    }

    fun onSelectQuote(quote: SwapProviderQuote) {
        swapQuoteService.selectQuote(quote)
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = SwapUiState(
                amountIn = swapQuoteState.amountIn,
                tokenIn = swapQuoteState.tokenIn,
                tokenOut = swapQuoteState.tokenOut,
                quoting = swapQuoteState.quoting,
                swapEnabled = isSwapEnabled(),
                quotes = swapQuoteState.quotes,
                preferredProvider = swapQuoteState.preferredProvider,
                quoteLifetime = swapQuoteState.quoteLifetime,
                quote = swapQuoteState.quote,
                error = swapQuoteState.error,
                availableBalance = swapQuoteState.availableBalance
            )
        }
    }

    private fun isSwapEnabled() = swapQuoteState.quote != null

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val swapQuoteService = SwapQuoteService(SwapProvidersManager(), App.adapterManager)
            return SwapViewModel(swapQuoteService) as T
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
