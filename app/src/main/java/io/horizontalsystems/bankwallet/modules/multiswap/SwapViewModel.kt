package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.SwapTermsManager
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.bankwallet.modules.multiswap.history.SwapRecordManager
import io.horizontalsystems.bankwallet.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.SwapHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import java.math.BigDecimal
import java.math.RoundingMode

@HiltViewModel(assistedFactory = SwapViewModel.Factory::class)
class SwapViewModel @AssistedInject constructor(
    @Assisted("tokenIn") private val tokenIn: Token?,
    @Assisted("tokenOut") private val tokenOut: Token?,
    private val currencyManager: CurrencyManager,
    private val swapTermsManager: SwapTermsManager,
    private val swapRecordManager: SwapRecordManager,
    private val marketKit: MarketKitWrapper,
    private val walletManager: WalletManager,
    private val adapterManager: IAdapterManager,
    connectivityManager: ConnectivityManager,
) : ViewModelUiState<SwapUiState>() {

    private val quoteService: SwapQuoteService
    private val balanceService: TokenBalanceService
    private val priceImpactService: PriceImpactService
    private val fiatServiceIn: FiatService
    private val fiatServiceOut: FiatService
    private val timerService: TimerService
    private val networkAvailabilityService: NetworkAvailabilityService
    private val defaultTokenService: SwapDefaultTokenService

    private val quoteLifetime = 20
    private val hasExplicitTokens = tokenIn != null || tokenOut != null
    private var tokensManuallySet = false

    private var networkState: NetworkAvailabilityService.State
    private var quoteState: SwapQuoteService.State
    private var balanceState: TokenBalanceService.State
    private var priceImpactState: PriceImpactService.State
    private var timerState: TimerService.State
    private var fiatAmountIn: BigDecimal? = null
    private var fiatAmountOut: BigDecimal? = null
    private var fiatAmountInputEnabled = false
    private var currency = currencyManager.baseCurrency
    private var requoteOnTimeout = true
    private var swapTermsAccepted = swapTermsManager.swapTermsAcceptedStateFlow.value
    private var amlChecking = false
    private var initialShowRegularPrice = true

    val amlCheckEventFlow = MutableSharedFlow<AmlCheckEvent>(extraBufferCapacity = 1)

    init {
        quoteService = SwapQuoteService()
        balanceService = TokenBalanceService(adapterManager)
        priceImpactService = PriceImpactService(PriceImpactLevel.Warning)
        fiatServiceIn = FiatService(marketKit)
        fiatServiceOut = FiatService(marketKit)
        timerService = TimerService()
        networkAvailabilityService = NetworkAvailabilityService(connectivityManager)
        defaultTokenService = SwapDefaultTokenService(marketKit, walletManager)

        networkState = networkAvailabilityService.stateFlow.value
        quoteState = quoteService.stateFlow.value
        balanceState = balanceService.stateFlow.value
        priceImpactState = priceImpactService.stateFlow.value
        timerState = timerService.stateFlow.value

        quoteService.start()

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
                fiatAmountInputEnabled = it.coinPrice != null && !it.coinPrice.expired
                fiatAmountIn = it.fiatAmount
                quoteService.setAmount(it.amount)
                priceImpactService.setAmountIn(fiatAmountIn)

                emitState()
            }
        }
        viewModelScope.launch {
            fiatServiceOut.stateFlow.collect {
                fiatAmountOut = it.fiatAmount

                priceImpactService.setAmountOut(fiatAmountOut)

                emitState()
            }
        }
        viewModelScope.launch {
            timerService.stateFlow.collect {
                timerState = it

                requoteIfTimeout()
            }
        }
        viewModelScope.launch {
            defaultTokenService.stateFlow.collect {
                if (tokenOut == null) {
                    it.tokenOut?.let { quoteService.setTokenOut(it) }
                }
            }
        }

        viewModelScope.launch {
            adapterManager.adaptersReadyObservable.asFlow().collect {
                balanceService.refresh()
            }
        }

        viewModelScope.launch {
            swapTermsManager.swapTermsAcceptedStateFlow.collect { accepted ->
                swapTermsAccepted = accepted
                emitState()
            }
        }

        fiatServiceIn.setCurrency(currency)
        fiatServiceOut.setCurrency(currency)

        viewModelScope.launch {
            currencyManager.baseCurrencyUpdatedFlow.collect {
                currency = currencyManager.baseCurrency
                fiatServiceIn.setCurrency(currency)
                fiatServiceOut.setCurrency(currency)
                emitState()
            }
        }

        networkAvailabilityService.start(viewModelScope)


        if (!hasExplicitTokens) {
            refreshDefaultTokens()
        } else {
            applyTokens(tokenIn, tokenOut)
        }
    }

    fun refreshDefaultTokens() {
        if (hasExplicitTokens || tokensManuallySet) return
        viewModelScope.launch(Dispatchers.IO) {
            val (resolvedIn, resolvedOut) = resolveDefaultTokens()
            applyTokens(resolvedIn, resolvedOut)
        }
    }

    private fun applyTokens(tokenIn: Token?, tokenOut: Token?) {
        tokenIn?.let {
            quoteService.setTokenIn(it)
            if (tokenOut == null) {
                defaultTokenService.setTokenIn(it)
            }
        }
        tokenOut?.let { quoteService.setTokenOut(it) }
    }

    private fun resolveDefaultTokens(): Pair<Token?, Token?> {
        val lastRecord = swapRecordManager.getAll().firstOrNull()
        return if (lastRecord != null) {
            val lastIn = TokenQuery.fromId(lastRecord.tokenInUid)?.let { marketKit.token(it) }
            val lastOut = TokenQuery.fromId(lastRecord.tokenOutUid)?.let { marketKit.token(it) }
            Pair(lastIn, lastOut)
        } else {
            // Default tokenIn — first token of the Swap "Popular Tokens" list (Bitcoin), so the
            // default stays in sync with whatever the token selector promotes to the top.
            val tokenIn = SwapPopularTokens.build(marketKit, null).firstOrNull()
                ?.let { activeWalletTokenFor(it) }
                ?: marketKit.token(TokenQuery(BlockchainType.Bitcoin, TokenType.Derived(TokenType.Derivation.Bip84)))
            val xmrToken = walletManager.activeWallets
                .firstOrNull { it.token.blockchainType == BlockchainType.Monero }
                ?.token
                ?: marketKit.token(TokenQuery(BlockchainType.Monero, TokenType.Native))
            Pair(tokenIn, xmrToken)
        }
    }

    // Prefer the user's active wallet for the same coin (keeps their chosen derivation),
    // falling back to the canonical token from the popular list.
    private fun activeWalletTokenFor(token: Token): Token =
        walletManager.activeWallets.firstOrNull {
            it.token.coin.uid == token.coin.uid && it.token.blockchainType == token.blockchainType
        }?.token ?: token

    private fun requoteIfTimeout() {
        if (requoteOnTimeout && timerState.timeout) {
            reQuote()
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
        error = networkState.error ?: quoteState.error ?: balanceState.error,
        availableBalance = balanceState.balance,
        fiatPriceImpact = priceImpactState.priceImpact,
        fiatPriceImpactLevel = priceImpactState.priceImpactLevel,
        fiatAmountIn = fiatAmountIn,
        fiatAmountOut = fiatAmountOut,
        currency = currency,
        fiatAmountInputEnabled = fiatAmountInputEnabled,
        needToAcceptTerms = !swapTermsAccepted && quoteState.quote?.provider?.requireTerms == true,
        amlChecking = amlChecking,
        initialShowRegularPrice = initialShowRegularPrice,
        swapTimeStatus = swapTimeStatus(
            quoteState.quote?.estimationTime,
            quoteState.quotes.map { it.estimationTime }
        ),
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

        priceImpactService.setProviderTitle(quoteState.quote?.provider?.title)

        fiatServiceIn.setToken(quoteState.tokenIn)
        fiatServiceIn.setAmount(quoteState.amountIn)
        fiatServiceOut.setToken(quoteState.tokenOut)
        fiatServiceOut.setAmount(quoteState.quote?.amountOut)

        quoteState.quote?.let {
            initialShowRegularPrice = it.amountIn <= it.amountOut
        }

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

    fun onSelectTokenIn(token: Token) {
        tokensManuallySet = true
        quoteService.setTokenIn(token)

        stat(page = StatPage.Swap, event = StatEvent.SwapSelectTokenIn(token))
    }

    fun onSelectTokenOut(token: Token) {
        tokensManuallySet = true
        quoteService.setTokenOut(token)

        stat(page = StatPage.Swap, event = StatEvent.SwapSelectTokenOut(token))
    }

    fun onSwitchPairs() {
        quoteService.switchPairs()

        stat(page = StatPage.Swap, event = StatEvent.SwapSwitchPairs)
    }

    fun onEnterFiatAmount(v: BigDecimal?) = fiatServiceIn.setFiatAmount(v)
    private fun reQuote() = quoteService.reQuote()
    fun onActionStarted(quote: SwapProviderQuote?) = quoteService.onActionStarted(quote)
    fun onActionCompleted() = quoteService.onActionCompleted()

    fun startProceed() {
        val provider = quoteState.quote?.provider ?: return
        val tokenIn = quoteState.tokenIn ?: return
        val amountIn = quoteState.amountIn ?: return

        if (!provider.amlPrecheck) {
            viewModelScope.launch { amlCheckEventFlow.emit(AmlCheckEvent.Proceed) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            amlChecking = true
            emitState()
            try {
                val addresses = SwapHelper.getSourceAddressesForAmlCheck(tokenIn, amountIn)
                val passedAmlCheck = if (addresses.isNotEmpty()) {
                    provider.checkAmlAddresses(addresses)
                } else {
                    throw IllegalStateException("No addresses found")
                }
                when (passedAmlCheck) {
                    true -> amlCheckEventFlow.emit(AmlCheckEvent.Proceed)
                    false -> amlCheckEventFlow.emit(AmlCheckEvent.RiskDetected)
                    null -> amlCheckEventFlow.emit(AmlCheckEvent.RiskUnknown)
                }
            } catch (e: Throwable) {
                amlCheckEventFlow.emit(AmlCheckEvent.Error(e))
            } finally {
                amlChecking = false
                emitState()
            }
        }
    }

    fun getCurrentQuote() = quoteState.quote
    fun onResume() {
        requoteOnTimeout = true
        quoteService.restart(::requoteIfTimeout)
    }

    fun onPause() {
        requoteOnTimeout = false
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("tokenIn") tokenIn: Token?,
            @Assisted("tokenOut") tokenOut: Token?,
        ): SwapViewModel
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
    val fiatAmountIn: BigDecimal?,
    val fiatAmountOut: BigDecimal?,
    val fiatPriceImpact: BigDecimal?,
    val currency: Currency,
    val fiatAmountInputEnabled: Boolean,
    val fiatPriceImpactLevel: PriceImpactLevel?,
    val needToAcceptTerms: Boolean,
    val amlChecking: Boolean,
    val initialShowRegularPrice: Boolean,
    val swapTimeStatus: SwapTimeStatus,
) {
    val currentStep: SwapStep = when {
        quoting -> SwapStep.Quoting
        error != null -> SwapStep.Error(error)
        tokenIn == null -> SwapStep.InputRequired(InputType.TokenIn)
        tokenOut == null -> SwapStep.InputRequired(InputType.TokenOut)
        amountIn == null -> SwapStep.InputRequired(InputType.Amount)
        amlChecking -> SwapStep.AmlChecking
        quote?.actionRequired != null -> SwapStep.ActionRequired(quote.actionRequired!!)
        else -> SwapStep.Proceed
    }
}

sealed class SwapStep {
    data class InputRequired(val inputType: InputType) : SwapStep()
    object Quoting : SwapStep()
    object AmlChecking : SwapStep()
    data class Error(val error: Throwable) : SwapStep()
    object Proceed : SwapStep()
    data class ActionRequired(val action: ISwapProviderAction) : SwapStep()
}

sealed class AmlCheckEvent {
    object Proceed : AmlCheckEvent()
    object RiskDetected : AmlCheckEvent()
    object RiskUnknown : AmlCheckEvent()
    data class Error(val error: Throwable) : AmlCheckEvent()
}

enum class InputType {
    TokenIn,
    TokenOut,
    Amount
}
