package io.horizontalsystems.walletkit.modules.multiswap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.core.managers.SwapTermsManager
import io.horizontalsystems.walletkit.core.managers.WalletManager
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.core.supports
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.modules.multiswap.action.ActionApprove
import io.horizontalsystems.walletkit.modules.multiswap.action.ActionRevoke
import io.horizontalsystems.walletkit.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapRecordManager
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapHelper
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

class SwapViewModel(
    private val quoteService: SwapQuoteService,
    private val balanceService: TokenBalanceService,
    private val priceImpactService: PriceImpactService,
    private val currencyManager: CurrencyManager,
    private val fiatServiceIn: FiatService,
    private val fiatServiceOut: FiatService,
    private val timerService: TimerService,
    private val networkAvailabilityService: NetworkAvailabilityService,
    private val defaultTokenService: SwapDefaultTokenService,
    private val swapTermsManager: SwapTermsManager,
    private val swapRecordManager: SwapRecordManager,
    private val marketKit: MarketKitWrapper,
    private val walletManager: WalletManager,
    private val adapterManager: IAdapterManager,
    private val accountManager: IAccountManager,
    tokenIn: Token?,
    tokenOut: Token? = null,
    private val allowancePolicy: ISwapAllowancePolicy? = null,
    // false = tokenOut stays empty until the user picks it explicitly
    private val autoResolveTokenOut: Boolean = true,
) : ViewModelUiState<SwapUiState>() {

    private val quoteLifetime = 20
    private val hasExplicitTokens = tokenIn != null || tokenOut != null
    private var tokensManuallySet = false

    private var networkState = networkAvailabilityService.stateFlow.value
    private var quoteState = quoteService.stateFlow.value
    private var balanceState = balanceService.stateFlow.value
    private var priceImpactState = priceImpactService.stateFlow.value
    private var timerState = timerService.stateFlow.value
    private var fiatAmountIn: BigDecimal? = null
    private var fiatAmountOut: BigDecimal? = null
    private var fiatAmountInputEnabled = false
    private var currency = currencyManager.baseCurrency
    private var requoteOnTimeout = true
    private var swapTermsAccepted = swapTermsManager.swapTermsAcceptedStateFlow.value
    private var amlChecking = false
    private var initialShowRegularPrice = true

    // External delivery address entered on SwapRecipientPage, remembered together with
    // the tokenOut it was entered for so it silently expires when the pair changes
    private var externalRecipientAddress: Address? = null
    private var externalRecipientToken: Token? = null

    val externalRecipient: Address?
        get() = externalRecipientAddress?.takeIf { externalRecipientToken == quoteState.tokenOut }

    fun setExternalRecipient(address: Address) {
        externalRecipientAddress = address
        externalRecipientToken = quoteState.tokenOut
    }

    val amlCheckEventFlow = MutableSharedFlow<AmlCheckEvent>(extraBufferCapacity = 1)

    init {
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
        if (autoResolveTokenOut) {
            viewModelScope.launch {
                defaultTokenService.stateFlow.collect {
                    if (tokenOut == null) {
                        it.tokenOut?.let { quoteService.setTokenOut(it) }
                    }
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
            applyTokens(resolvedIn, if (autoResolveTokenOut) resolvedOut else null)
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
        val (resolvedIn, resolvedOut) = if (lastRecord != null) {
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

        // The defaults above (and a swap record left by another account) may name coins the
        // active account can never hold — e.g. BTC for a Monero-only or watch account. The
        // sell side must be signable, so fall back to the account's first enabled wallet.
        // The buy side may stay unsupported: such swaps are delivered to an external
        // recipient address entered before confirmation.
        val accountType = accountManager.activeAccount?.type ?: return Pair(resolvedIn, resolvedOut)
        val tokenIn = resolvedIn?.takeIf { it.supportedBy(accountType) }
            ?: walletManager.activeWallets.firstOrNull()?.token
        val tokenOut = resolvedOut?.takeIf { it != tokenIn }
        return Pair(tokenIn, tokenOut)
    }

    private fun Token.supportedBy(accountType: AccountType): Boolean =
        supports(accountType) && blockchainType.supports(accountType)

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
            quoteState.quote?.provider?.type
        ),
        allowanceActionSuppressed = allowanceActionSuppressed(),
        externalRecipientRequired = externalRecipientRequired(quoteState.tokenOut),
    )

    // tokenOut the account can't hold: the swap is deliverable only to an external
    // address, which the user is asked for before the confirmation screen
    private fun externalRecipientRequired(tokenOut: Token?): Boolean {
        if (tokenOut == null) return false
        val accountType = accountManager.activeAccount?.type ?: return false
        return !tokenOut.supportedBy(accountType)
    }

    // The caller's policy can declare approve/revoke unnecessary (the execution
    // layer owns the allowance); other action types are never suppressed.
    private fun allowanceActionSuppressed(): Boolean {
        val action = quoteState.quote?.actionRequired ?: return false
        if (action !is ActionApprove && action !is ActionRevoke) return false
        val tokenIn = quoteState.tokenIn ?: return false
        val amountIn = quoteState.amountIn ?: return false
        return allowancePolicy?.allowanceNotRequired(tokenIn, amountIn) == true
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
        // picking the current sell token on the You Get side swaps the pair — blocked
        // when the current tokenOut can't be signed by the account (same rule as the
        // switch arrow), else an externally-delivered token would become the sell side
        if (token == quoteState.tokenIn && externalRecipientRequired(quoteState.tokenOut)) return

        tokensManuallySet = true
        quoteService.setTokenOut(token)

        stat(page = StatPage.Swap, event = StatEvent.SwapSelectTokenOut(token))
    }

    fun onSwitchPairs() {
        // an externally-delivered tokenOut can't become the sell side — the account
        // can't sign transactions for it
        if (externalRecipientRequired(quoteState.tokenOut)) return

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

    class Factory(
        private val tokenIn: Token?,
        private val tokenOut: Token? = null,
        // null = the full provider registry
        private val providers: List<IMultiSwapProvider>? = null,
        // null = stock behavior (approve/revoke steps surface as usual)
        private val allowancePolicy: ISwapAllowancePolicy? = null,
        // false = tokenOut stays empty until the user picks it explicitly
        private val autoResolveTokenOut: Boolean = true,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val swapQuoteService = providers?.let { SwapQuoteService(it) } ?: SwapQuoteService()
            val tokenBalanceService = TokenBalanceService(App.adapterManager)
            val priceImpactService = PriceImpactService(PriceImpactLevel.Warning)

            return SwapViewModel(
                swapQuoteService,
                tokenBalanceService,
                priceImpactService,
                App.currencyManager,
                FiatService(App.marketKit),
                FiatService(App.marketKit),
                TimerService(),
                NetworkAvailabilityService(App.connectivityManager),
                SwapDefaultTokenService(App.marketKit, App.walletManager),
                App.swapTermsManager,
                App.swapRecordManager,
                App.marketKit,
                App.walletManager,
                App.adapterManager,
                App.accountManager,
                tokenIn,
                tokenOut,
                allowancePolicy,
                autoResolveTokenOut,
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
    val allowanceActionSuppressed: Boolean = false,
    val externalRecipientRequired: Boolean = false,
) {
    val currentStep: SwapStep = when {
        quoting -> SwapStep.Quoting
        error != null -> SwapStep.Error(error)
        tokenIn == null -> SwapStep.InputRequired(InputType.TokenIn)
        tokenOut == null -> SwapStep.InputRequired(InputType.TokenOut)
        amountIn == null -> SwapStep.InputRequired(InputType.Amount)
        amlChecking -> SwapStep.AmlChecking
        quote?.actionRequired != null && !allowanceActionSuppressed -> SwapStep.ActionRequired(quote.actionRequired!!)
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
