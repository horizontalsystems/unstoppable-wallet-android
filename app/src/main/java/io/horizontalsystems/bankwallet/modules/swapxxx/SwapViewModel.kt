package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swapxxx.providers.ISwapXxxProvider
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.SwapSettingsService
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapViewModel(
    private val quoteService: SwapQuoteService,
    private val balanceService: TokenBalanceService,
    private val settingsService: SwapSettingsService,
    private val priceImpactService: PriceImpactService
) : ViewModel() {

    private var quoteState = quoteService.stateFlow.value
    private var availableBalance = balanceService.balanceFlow.value
    private var priceImpactState = priceImpactService.stateFlow.value

    var uiState: SwapUiState by mutableStateOf(
        SwapUiState(
            amountIn = quoteState.amountIn,
            tokenIn = quoteState.tokenIn,
            tokenOut = quoteState.tokenOut,
            quoting = quoteState.quoting,
            swapEnabled = isSwapEnabled(),
            quotes = quoteState.quotes,
            preferredProvider = quoteState.preferredProvider,
            quoteLifetime = quoteState.quoteLifetime,
            quote = quoteState.quote,
            error = quoteState.error,
            availableBalance = availableBalance,
            priceImpact = priceImpactState.priceImpact,
            priceImpactLevel = priceImpactState.priceImpactLevel,
            priceImpactCaution = priceImpactState.priceImpactCaution,
        )
    )
        private set

    init {
        viewModelScope.launch {
            quoteService.stateFlow.collect {
                handleUpdatedQuoteState(it)
            }
        }
        viewModelScope.launch {
            balanceService.balanceFlow.collect {
                handleUpdatedBalance(it)
            }
        }
        viewModelScope.launch {
            priceImpactService.stateFlow.collect {
                handleUpdatedPriceImpactState(it)
            }
        }
    }

    private fun handleUpdatedBalance(balance: BigDecimal?) {
        this.availableBalance = balance

        emitState()
    }

    private fun handleUpdatedQuoteState(quoteState: SwapQuoteService.State) {
        this.quoteState = quoteState

        balanceService.setToken(quoteState.tokenIn)
        priceImpactService.setPriceImpact(quoteState.quote?.priceImpact, quoteState.quote?.provider?.title)

        emitState()
    }

    private fun handleUpdatedPriceImpactState(priceImpactState: PriceImpactService.State) {
        this.priceImpactState = priceImpactState

        emitState()
    }

    fun onEnterAmount(v: BigDecimal?) {
        quoteService.setAmount(v)
    }

    fun onSelectTokenIn(token: Token) {
        quoteService.setTokenIn(token)
    }

    fun onSelectTokenOut(token: Token) {
        quoteService.setTokenOut(token)
    }

    fun onSwitchPairs() {
        quoteService.switchPairs()
    }

    fun onSelectQuote(quote: SwapProviderQuote) {
        quoteService.selectQuote(quote)
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = SwapUiState(
                amountIn = quoteState.amountIn,
                tokenIn = quoteState.tokenIn,
                tokenOut = quoteState.tokenOut,
                quoting = quoteState.quoting,
                swapEnabled = isSwapEnabled(),
                quotes = quoteState.quotes,
                preferredProvider = quoteState.preferredProvider,
                quoteLifetime = quoteState.quoteLifetime,
                quote = quoteState.quote,
                error = quoteState.error,
                availableBalance = availableBalance,
                priceImpact = priceImpactState.priceImpact,
                priceImpactLevel = priceImpactState.priceImpactLevel,
                priceImpactCaution = priceImpactState.priceImpactCaution
            )
        }
    }

    private fun isSwapEnabled() = quoteState.quote != null


    var saveSettingsEnabled by mutableStateOf(settingsService.saveEnabledFlow.value)

    init {
        viewModelScope.launch {
            settingsService.saveEnabledFlow.collect {
                saveSettingsEnabled = it
            }
        }
    }

    fun onSettingError(id: String, error: Throwable?) {
        settingsService.onSettingError(id, error)
    }

    fun onSettingEnter(id: String, value: Any?) {
        settingsService.setSetting(id, value)
    }

    fun saveSettings() {
        settingsService.save()
        quoteService.setSwapSettings(settingsService.getSettings())
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val swapQuoteService = SwapQuoteService(SwapProvidersManager())
            val tokenBalanceService = TokenBalanceService(App.adapterManager)
            val priceImpactService = PriceImpactService()

            val swapSettingsService = SwapSettingsService()

            return SwapViewModel(swapQuoteService, tokenBalanceService, swapSettingsService, priceImpactService) as T
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
    val preferredProvider: ISwapXxxProvider?,
    val quoteLifetime: Long,
    val quote: SwapProviderQuote?,
    val error: Throwable?,
    val availableBalance: BigDecimal?,
    val priceImpact: BigDecimal?,
    val priceImpactLevel: SwapMainModule.PriceImpactLevel?,
    val priceImpactCaution: HSCaution?
)
