package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.bankwallet.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class SwapViewModel(
    private val quoteService: SwapQuoteService,
    private val balanceService: TokenBalanceService,
    private val priceImpactService: PriceImpactService,
    private val currencyManager: CurrencyManager,
    private val fiatServiceIn: FiatService,
    private val fiatServiceOut: FiatService,
    private val timerService: TimerService,
    private val networkAvailabilityService: NetworkAvailabilityService,
    tokenIn: Token?
) : ViewModelUiState<SwapUiState>() {

    private val quoteLifetime = 20

    private var networkState = networkAvailabilityService.stateFlow.value
    private var quoteState = quoteService.stateFlow.value
    private var balanceState = balanceService.stateFlow.value
    private var priceImpactState = priceImpactService.stateFlow.value
    private var timerState = timerService.stateFlow.value
    private var fiatAmountIn: BigDecimal? = null
    private var fiatAmountOut: BigDecimal? = null
    private var fiatAmountInputEnabled = false
    private val currency = currencyManager.baseCurrency

    init {
        viewModelScope.launch {
            networkAvailabilityService.stateFlow.collect {
                handleUpdatedNetworkState(it)
            }
        }
        viewModelScope.launch {
            quoteService.stateFlow.collect {
                handleUpdatedQuoteState(it)
            }
        }
        viewModelScope.launch {
            balanceService.stateFlow.collect {
                handleUpdatedBalanceState(it)
            }
        }
        viewModelScope.launch {
            priceImpactService.stateFlow.collect {
                handleUpdatedPriceImpactState(it)
            }
        }
        viewModelScope.launch {
            fiatServiceIn.stateFlow.collect {
                fiatAmountInputEnabled = it.coinPrice != null
                fiatAmountIn = it.fiatAmount
                quoteService.setAmount(it.amount)
                priceImpactService.setFiatAmountIn(fiatAmountIn)

                emitState()
            }
        }
        viewModelScope.launch {
            fiatServiceOut.stateFlow.collect {
                fiatAmountOut = it.fiatAmount

                priceImpactService.setFiatAmountOut(fiatAmountOut)

                emitState()
            }
        }
        viewModelScope.launch {
            timerService.stateFlow.collect {
                timerState = it

                emitState()
            }
        }

        fiatServiceIn.setCurrency(currency)
        fiatServiceOut.setCurrency(currency)
        networkAvailabilityService.start(viewModelScope)
        tokenIn?.let {
            quoteService.setTokenIn(it)
        }
    }

    override fun createState() = SwapUiState(
        amountIn = quoteState.amountIn,
        tokenIn = quoteState.tokenIn,
        tokenOut = quoteState.tokenOut,
        quoting = quoteState.quoting,
        quotes = quoteState.quotes,
        preferredProvider = quoteState.preferredProvider,
        quote = quoteState.quote,
        error = networkState.error ?: quoteState.error ?: balanceState.error ?: priceImpactState.error,
        availableBalance = balanceState.balance,
        priceImpact = priceImpactState.priceImpact,
        priceImpactLevel = priceImpactState.priceImpactLevel,
        priceImpactCaution = priceImpactState.priceImpactCaution,
        fiatPriceImpact = priceImpactState.fiatPriceImpact,
        fiatPriceImpactLevel = priceImpactState.fiatPriceImpactLevel,
        fiatAmountIn = fiatAmountIn,
        fiatAmountOut = fiatAmountOut,
        currency = currency,
        fiatAmountInputEnabled = fiatAmountInputEnabled,
        timeRemaining = timerState.remaining,
        timeout = timerState.timeout,
        timeRemainingProgress = timerState.remaining?.let { remaining ->
            remaining / quoteLifetime.toFloat()
        }
    )

    private fun handleUpdatedNetworkState(networkState: NetworkAvailabilityService.State) {
        this.networkState = networkState

        emitState()

        if (networkState.networkAvailable && quoteState.error != null) {
            reQuote()
        }
    }

    private fun handleUpdatedBalanceState(balanceState: TokenBalanceService.State) {
        this.balanceState = balanceState

        emitState()
    }

    private fun handleUpdatedQuoteState(quoteState: SwapQuoteService.State) {
        this.quoteState = quoteState

        balanceService.setToken(quoteState.tokenIn)
        balanceService.setAmount(quoteState.amountIn)

        priceImpactService.setPriceImpact(quoteState.quote?.priceImpact, quoteState.quote?.provider?.title)

        fiatServiceIn.setToken(quoteState.tokenIn)
        fiatServiceIn.setAmount(quoteState.amountIn)
        fiatServiceOut.setToken(quoteState.tokenOut)
        fiatServiceOut.setAmount(quoteState.quote?.amountOut)

        emitState()

        if (quoteState.quote != null) {
            val elapsedMillis = System.currentTimeMillis() - quoteState.quote.createdAt
            val remainingSeconds = (quoteLifetime - elapsedMillis / 1000).coerceAtLeast(0)
            timerService.start(remainingSeconds)
        } else {
            timerService.reset()
        }
    }

    private fun handleUpdatedPriceImpactState(priceImpactState: PriceImpactService.State) {
        this.priceImpactState = priceImpactState

        emitState()
    }

    fun onSelectQuote(quote: SwapProviderQuote) {
        quoteService.selectQuote(quote)
    }

    fun onEnterAmount(v: BigDecimal?) = quoteService.setAmount(v)
    fun onEnterAmountPercentage(percentage: Int) {
        val tokenIn = quoteState.tokenIn ?: return
        val availableBalance = balanceState.balance ?: return

        val amount = availableBalance
            .times(BigDecimal(percentage / 100.0))
            .setScale(tokenIn.decimals, RoundingMode.DOWN)
            .stripTrailingZeros()

        quoteService.setAmount(amount)
    }
    fun onSelectTokenIn(token: Token) = quoteService.setTokenIn(token)
    fun onSelectTokenOut(token: Token) = quoteService.setTokenOut(token)
    fun onSwitchPairs() = quoteService.switchPairs()
    fun onUpdateSettings(settings: Map<String, Any?>) = quoteService.setSwapSettings(settings)
    fun onEnterFiatAmount(v: BigDecimal?) = fiatServiceIn.setFiatAmount(v)
    fun reQuote() = quoteService.reQuote()
    fun onActionStarted() = quoteService.onActionStarted()
    fun onActionCompleted() = quoteService.onActionCompleted()

    fun getCurrentQuote() = quoteState.quote
    fun getSettings() = quoteService.getSwapSettings()

    class Factory(private val tokenIn: Token?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val swapQuoteService = SwapQuoteService()
            val tokenBalanceService = TokenBalanceService(App.adapterManager)
            val priceImpactService = PriceImpactService()

            return SwapViewModel(
                swapQuoteService,
                tokenBalanceService,
                priceImpactService,
                App.currencyManager,
                FiatService(App.marketKit),
                FiatService(App.marketKit),
                TimerService(),
                NetworkAvailabilityService(App.connectivityManager),
                tokenIn
            ) as T
        }
    }
}

data class SwapUiState(
    val amountIn: BigDecimal?,
    val tokenIn: Token?,
    val tokenOut: Token?,
    val quoting: Boolean,
    val quotes: List<SwapProviderQuote>,
    val preferredProvider: IMultiSwapProvider?,
    val quote: SwapProviderQuote?,
    val error: Throwable?,
    val availableBalance: BigDecimal?,
    val priceImpact: BigDecimal?,
    val priceImpactLevel: SwapMainModule.PriceImpactLevel?,
    val priceImpactCaution: HSCaution?,
    val fiatAmountIn: BigDecimal?,
    val fiatAmountOut: BigDecimal?,
    val fiatPriceImpact: BigDecimal?,
    val currency: Currency,
    val fiatAmountInputEnabled: Boolean,
    val fiatPriceImpactLevel: SwapMainModule.PriceImpactLevel?,
    val timeRemaining: Long?,
    val timeout: Boolean,
    val timeRemainingProgress: Float?
) {
    val currentStep: SwapStep = when {
        quoting -> SwapStep.Quoting
        error != null -> SwapStep.Error(error)
        tokenIn == null -> SwapStep.InputRequired(InputType.TokenIn)
        tokenOut == null -> SwapStep.InputRequired(InputType.TokenOut)
        amountIn == null -> SwapStep.InputRequired(InputType.Amount)
        quote?.actionRequired != null -> SwapStep.ActionRequired(quote.actionRequired!!)
        else -> SwapStep.Proceed
    }
}

sealed class SwapStep {
    data class InputRequired(val inputType: InputType) : SwapStep()
    object Quoting : SwapStep()
    data class Error(val error: Throwable) : SwapStep()
    object Proceed : SwapStep()
    data class ActionRequired(val action: ISwapProviderAction) : SwapStep()
}

enum class InputType {
    TokenIn,
    TokenOut,
    Amount
}
