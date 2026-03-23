package cash.p.terminal.modules.multiswap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.moreThanZero
import cash.p.terminal.modules.multiswap.action.ISwapProviderAction
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import cash.p.terminal.wallet.useCases.WalletUseCase
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.ViewModelUiState
import io.horizontalsystems.core.entities.Currency
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal
import java.math.RoundingMode

class SwapViewModel(
    private val quoteService: SwapQuoteService,
    private val balanceService: TokenBalanceService,
    private val priceImpactService: PriceImpactService,
    currencyManager: CurrencyManager,
    private val fiatServiceIn: FiatService,
    private val fiatServiceOut: FiatService,
    private val timerService: TimerService,
    private val networkAvailabilityService: NetworkAvailabilityService,
    private val marketKit: MarketKitWrapper,
    tokenIn: Token?,
    tokenOut: Token?
) : ViewModelUiState<SwapUiState>() {

    private val quoteLifetime = 20

    private var networkState = networkAvailabilityService.stateFlow.value
    private var quoteState = quoteService.stateFlow.value
    private var balanceState = balanceService.stateFlow.value
    private var priceImpactState = priceImpactService.stateFlow.value
    private var timerState = timerService.stateFlow.value

    var timeRemainingProgress by mutableStateOf<Float?>(null)
        private set

    private var fiatAmountIn: BigDecimal? = null
    private var fiatAmountOut: BigDecimal? = null
    private var fiatAmountInputEnabled = false
    private val currency = currencyManager.baseCurrency
    private val balanceHiddenManager: IBalanceHiddenManager by inject(IBalanceHiddenManager::class.java)
    private val walletUseCase: WalletUseCase by inject(WalletUseCase::class.java)
    private var warningMessage: TranslatableString? = null

    init {
        viewModelScope.launch {
            quoteService.start()
        }

        viewModelScope.launch {
            balanceHiddenManager.anyWalletVisibilityChangedFlow.collect {
                emitState()
            }
        }
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
                val prevTimeout = timerState.timeout
                timerState = it

                timeRemainingProgress = it.remaining?.let { remaining ->
                    remaining / quoteLifetime.toFloat()
                }

                if (it.timeout != prevTimeout) {
                    emitState()
                }
            }
        }

        fiatServiceIn.setCurrency(currency)
        addCloseable(fiatServiceIn)
        fiatServiceOut.setCurrency(currency)
        addCloseable(fiatServiceOut)
        networkAvailabilityService.start(viewModelScope)
        tokenIn?.let {
            quoteService.setTokenIn(it)
        }
        tokenOut?.let {
            quoteService.setTokenOut(it)
        }
    }

    override fun createState(): SwapUiState {
        val feeToken = balanceState.feeToken
        val fee = balanceState.fee
        val networkFeeFiatAmount = if (feeToken != null && fee != null) {
            marketKit.coinPrice(feeToken.coin.uid, currency.code)?.let { coinPrice ->
                fee * coinPrice.value
            }
        } else null

        return SwapUiState(
            amountIn = quoteState.amountIn,
            tokenIn = quoteState.tokenIn,
            tokenOut = quoteState.tokenOut,
            quoting = quoteState.quoting,
            quotes = quoteState.quotes,
            preferredProvider = quoteState.preferredProvider,
            quote = quoteState.quote,
            error = networkState.error ?: quoteState.error ?: balanceState.error
                ?: priceImpactState.error,
            availableBalance = balanceState.balance,
            displayBalance = balanceState.displayBalance,
            networkFee = fee,
            networkFeeFiatAmount = networkFeeFiatAmount,
            feeToken = feeToken,
            feeCoinBalance = balanceState.feeCoinBalance,
            insufficientFeeBalance = balanceState.insufficientFeeBalance,
            balanceHidden = quoteState.tokenIn?.let {
                balanceHiddenManager.isWalletBalanceHidden(it.tokenQuery.id)
            } ?: balanceHiddenManager.balanceHidden,
            warningMessage = warningMessage,
            priceImpact = priceImpactState.priceImpact,
            priceImpactLevel = priceImpactState.priceImpactLevel,
            priceImpactCaution = priceImpactState.priceImpactCaution,
            fiatAmountIn = fiatAmountIn,
            fiatAmountOut = fiatAmountOut,
            fiatPriceImpact = priceImpactState.fiatPriceImpact,
            currency = currency,
            fiatAmountInputEnabled = fiatAmountInputEnabled,
            fiatPriceImpactLevel = priceImpactState.fiatPriceImpactLevel,
            timeout = timerState.timeout,
            multiSwapRoute = quoteState.multiSwapRoute,
        )
    }

    private fun fetchWarningMessageAsync() {
        viewModelScope.launch {
            warningMessage = obtainWarningMessage()
            emitState() // Update UI again once warning is fetched
        }
    }

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

        priceImpactService.setPriceImpact(
            quoteState.quote?.priceImpact?.negate(),
            quoteState.quote?.provider?.title
        )

        fiatServiceIn.setToken(quoteState.tokenIn)
        fiatServiceIn.setAmount(quoteState.amountIn)
        fiatServiceOut.setToken(quoteState.tokenOut)
        val finalAmountOut = quoteState.multiSwapRoute?.selectedLeg2Quote?.amountOut
            ?: quoteState.quote?.amountOut
        fiatServiceOut.setAmount(finalAmountOut)

        emitState() // Emit immediately so UI updates without waiting for warning
        fetchWarningMessageAsync()

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

    fun onSelectTokenIn(token: Token) {
        quoteService.setTokenIn(token)
    }

    fun onSelectTokenOut(token: Token) {
        quoteService.setTokenOut(token)
    }

    fun onSwitchPairs() {
        quoteService.switchPairs()
    }

    fun toggleHideBalance() {
        HudHelper.vibrate(App.instance)
        val tokenIn = quoteState.tokenIn
        if (tokenIn != null) {
            balanceHiddenManager.toggleWalletBalanceHidden(tokenIn.tokenQuery.id)
        } else {
            balanceHiddenManager.toggleBalanceHidden()
        }
        emitState()
    }

    fun createMissingTokens(tokens: Set<Token>) {
        viewModelScope.launch {
            walletUseCase.awaitWallets(tokens)
            reQuote()
        }
    }

    fun onSelectLeg1Quote(quote: SwapProviderQuote) = quoteService.selectLeg1Quote(quote)
    fun onSelectLeg2Quote(quote: SwapProviderQuote) = quoteService.selectLeg2Quote(quote)

    fun onUpdateSettings(settings: Map<String, Any?>) = quoteService.setSwapSettings(settings)
    fun onEnterFiatAmount(v: BigDecimal?) = fiatServiceIn.setFiatAmount(v)
    fun reQuote() = quoteService.reQuote()
    fun onActionStarted() = quoteService.onActionStarted()
    fun onActionCompleted() = quoteService.onActionCompleted()

    fun getCurrentQuote() = quoteState.quote
    fun getSettings() = quoteService.getSwapSettings()

    private suspend fun obtainWarningMessage(): TranslatableString? {
        val quote = quoteState.quote ?: return null

        return quote.provider.getWarningMessage(quote.tokenIn, quote.tokenOut)
    }

    class Factory(private val tokenIn: Token?, private val tokenOut: Token?) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val swapQuoteService: SwapQuoteService by inject(SwapQuoteService::class.java)
            val marketKit: MarketKitWrapper by inject(MarketKitWrapper::class.java)
            val tokenBalanceService = TokenBalanceService(App.adapterManager, marketKit)
            val priceImpactService = PriceImpactService()

            return SwapViewModel(
                quoteService = swapQuoteService,
                balanceService = tokenBalanceService,
                priceImpactService = priceImpactService,
                currencyManager = App.currencyManager,
                fiatServiceIn = FiatService(marketKit),
                fiatServiceOut = FiatService(marketKit),
                timerService = TimerService(),
                networkAvailabilityService = NetworkAvailabilityService(App.connectivityManager),
                marketKit = marketKit,
                tokenIn = tokenIn,
                tokenOut = tokenOut
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
    val displayBalance: BigDecimal?,
    val networkFee: BigDecimal?,
    val networkFeeFiatAmount: BigDecimal?,
    val feeToken: Token?,
    val feeCoinBalance: BigDecimal?,
    val insufficientFeeBalance: Boolean,
    val balanceHidden: Boolean,
    val warningMessage: TranslatableString?,
    val priceImpact: BigDecimal?,
    val priceImpactLevel: PriceImpactLevel?,
    val priceImpactCaution: HSCaution?,
    val fiatAmountIn: BigDecimal?,
    val fiatAmountOut: BigDecimal?,
    val fiatPriceImpact: BigDecimal?,
    val currency: Currency,
    val fiatAmountInputEnabled: Boolean,
    val fiatPriceImpactLevel: PriceImpactLevel?,
    val timeout: Boolean,
    val multiSwapRoute: MultiSwapRoute?,
) {
    val currentStep: SwapStep = when {
        error != null -> SwapStep.Error(error)
        quoting || (allChosen() && amountIn.moreThanZero() && !fiatAmountOut.moreThanZero()) -> SwapStep.Quoting
        tokenIn == null -> SwapStep.InputRequired(InputType.TokenIn)
        tokenOut == null -> SwapStep.InputRequired(InputType.TokenOut)
        amountIn == null || amountIn.compareTo(BigDecimal.ZERO) == 0 -> SwapStep.InputRequired(
            InputType.Amount
        )

        quote?.actionRequired != null -> SwapStep.ActionRequired(requireNotNull(quote.actionRequired))
        else -> SwapStep.Proceed
    }

    private fun allChosen() =
        tokenIn != null && tokenOut != null
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
