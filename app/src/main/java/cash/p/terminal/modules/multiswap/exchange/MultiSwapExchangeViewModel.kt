package cash.p.terminal.modules.multiswap.exchange

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.storage.PendingMultiSwapStorage
import cash.p.terminal.core.usecase.FetchSwapQuotesUseCase
import cash.p.terminal.core.usecase.SyncPendingMultiSwapUseCase
import cash.p.terminal.entities.PendingMultiSwap
import cash.p.terminal.modules.multiswap.MultiSwapOnChainMonitor
import cash.p.terminal.modules.multiswap.PriceImpactLevel
import cash.p.terminal.modules.multiswap.SwapProviderQuote
import cash.p.terminal.modules.multiswap.SwapQuoteService
import cash.p.terminal.modules.multiswap.TimerService
import cash.p.terminal.core.ServiceStateFlow
import cash.p.terminal.modules.multiswap.TokenBalanceService
import cash.p.terminal.modules.multiswap.providers.ChangeNowProvider
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.modules.multiswap.providers.QuickexProvider
import cash.p.terminal.modules.multiswap.action.ActionCreate
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.badge
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import cash.p.terminal.wallet.useCases.WalletUseCase
import cash.p.terminal.wallet.coinImageUrl
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.Currency
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal

class MultiSwapExchangeViewModel(
    private val pendingMultiSwapId: String,
    private val pendingMultiSwapStorage: PendingMultiSwapStorage,
    private val marketKit: MarketKitWrapper,
    private val numberFormatter: IAppNumberFormatter,
    private val onChainMonitor: MultiSwapOnChainMonitor,
    private val swapQuoteService: SwapQuoteService,
    private val fetchSwapQuotesUseCase: FetchSwapQuotesUseCase,
    private val timerService: TimerService,
    private val syncPendingMultiSwapUseCase: SyncPendingMultiSwapUseCase,
    private val syncIntervalMs: Long = SYNC_INTERVAL_MS,
    currencyManager: CurrencyManager,
    adapterManager: IAdapterManager,
    private val balanceHiddenManager: IBalanceHiddenManager,
    private val walletManager: IWalletManager,
    private val walletUseCase: WalletUseCase,
) : ViewModel() {

    var uiState by mutableStateOf<MultiSwapExchangeUiState?>(null)
        private set

    var closeScreen by mutableStateOf(false)
        private set

    // Leg2 quote state
    var leg2Quotes by mutableStateOf<List<SwapProviderQuote>>(emptyList())
        private set
    var selectedLeg2Quote by mutableStateOf<SwapProviderQuote?>(null)
        private set

    val leg1NavigationRecordUid: String?
        get() = currentSwap?.let { it.leg1InfoRecordUid ?: it.leg1TransactionId }

    private var currentSwap: PendingMultiSwap? = null
    private val currency = currencyManager.baseCurrency
    private val tokenBalanceService = TokenBalanceService(adapterManager, marketKit)

    val leg2BalanceStateFlow: ServiceStateFlow<TokenBalanceService.State>
        get() = tokenBalanceService.stateFlow

    var leg2BalanceHidden by mutableStateOf(balanceHiddenManager.balanceHidden)
        private set
    private var monitoringLeg1 = false
    private var monitoringLeg2 = false
    private var periodicSyncStarted = false
    private val syncMutex = kotlinx.coroutines.sync.Mutex()
    private var leg2QuotingJob: Job? = null
    private var leg2QuoteFetched = false
    private var leg2Quoting = false
    private var creatingWallets = false
    private var timerState = timerService.stateFlow.value

    var timeRemainingProgress by mutableStateOf<Float?>(null)
        private set

    init {
        observeSwap()
        viewModelScope.launch {
            timerService.stateFlow.collect {
                val prevTimeout = timerState.timeout
                timerState = it
                timeRemainingProgress = it.remaining?.let { remaining ->
                    remaining / QUOTE_LIFETIME.toFloat()
                }
                currentSwap?.let { swap -> uiState = mapToUiState(swap) }

                if (it.timeout && !prevTimeout) {
                    refreshQuotes()
                }
            }
        }
        viewModelScope.launch {
            balanceHiddenManager.anyWalletVisibilityChangedFlow.collect {
                updateLeg2BalanceHidden()
            }
        }
        viewModelScope.launch {
            walletManager.activeWalletsFlow.collect {
                currentSwap?.let { swap -> uiState = mapToUiState(swap) }
            }
        }
    }

    private fun observeSwap() {
        viewModelScope.launch {
            pendingMultiSwapStorage.getAll().collect { swaps ->
                val swap = swaps.firstOrNull { it.id == pendingMultiSwapId }
                if (swap == null) {
                    if (currentSwap != null) {
                        currentSwap = null
                        closeScreen = true
                    }
                    return@collect
                }

                val previousSwap = currentSwap
                currentSwap = swap
                uiState = mapToUiState(swap)
                startMonitoringIfNeeded(swap)
                startPeriodicSyncIfNeeded()

                // Fetch leg2 quotes when leg1 transitions to completed
                val leg1JustCompleted = previousSwap != null
                    && previousSwap.leg1Status != PendingMultiSwap.STATUS_COMPLETED
                    && swap.leg1Status == PendingMultiSwap.STATUS_COMPLETED

                val leg1AlreadyCompleted = swap.leg1Status == PendingMultiSwap.STATUS_COMPLETED
                    && swap.leg2Status == PendingMultiSwap.STATUS_PENDING

                if (leg1JustCompleted || (leg1AlreadyCompleted && !leg2QuoteFetched)) {
                    fetchLeg2Quotes(swap)
                }
            }
        }
    }

    private fun fetchLeg2Quotes(swap: PendingMultiSwap) {
        val tokenIn = resolveToken(swap.coinUidIntermediate, swap.blockchainTypeIntermediate) ?: return
        val tokenOut = resolveToken(swap.coinUidOut, swap.blockchainTypeOut) ?: return
        val amountIn = swap.leg1AmountOut ?: return

        tokenBalanceService.setToken(tokenIn)
        tokenBalanceService.setAmount(amountIn)
        updateLeg2BalanceHidden()

        leg2QuotingJob?.cancel()
        leg2QuoteFetched = true
        leg2Quoting = true
        leg2Quotes = emptyList()
        selectedLeg2Quote = null
        timerService.reset()
        currentSwap?.let { uiState = mapToUiState(it) }
        leg2QuotingJob = viewModelScope.launch {
            try {
                swapQuoteService.start()
                val quotes = fetchSwapQuotesUseCase(
                    providers = swapQuoteService.providers,
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    amountIn = amountIn,
                )
                leg2Quoting = false
                leg2Quotes = quotes
                selectedLeg2Quote = quotes.firstOrNull()
                startTimerIfNeeded()
                currentSwap?.let { uiState = mapToUiState(it) }
            } catch (e: Exception) {
                leg2Quoting = false
                Timber.e(e, "Failed to fetch leg2 quotes")
                currentSwap?.let { uiState = mapToUiState(it) }
            }
        }
    }

    fun onSelectLeg2Quote(quote: SwapProviderQuote) {
        selectedLeg2Quote = quote
        startTimerIfNeeded()
        currentSwap?.let { uiState = mapToUiState(it) }
    }

    fun toggleLeg2BalanceHidden() {
        val tokenIn = currentSwap?.let { resolveToken(it.coinUidIntermediate, it.blockchainTypeIntermediate) }
        if (tokenIn != null) {
            balanceHiddenManager.toggleWalletBalanceHidden(tokenIn.tokenQuery.id)
        } else {
            balanceHiddenManager.toggleBalanceHidden()
        }
        updateLeg2BalanceHidden()
    }

    private fun updateLeg2BalanceHidden() {
        val tokenIn = currentSwap?.let { resolveToken(it.coinUidIntermediate, it.blockchainTypeIntermediate) }
        leg2BalanceHidden = tokenIn?.let { balanceHiddenManager.isWalletBalanceHidden(it.tokenQuery.id) }
            ?: balanceHiddenManager.balanceHidden
    }

    private fun startMonitoringIfNeeded(swap: PendingMultiSwap) {
        if (swap.leg1Status == PendingMultiSwap.STATUS_EXECUTING && !monitoringLeg1) {
            monitoringLeg1 = onChainMonitor.observeBalanceIncrease(
                coinUid = swap.coinUidIntermediate,
                blockchainType = BlockchainType.fromUid(swap.blockchainTypeIntermediate),
                scope = viewModelScope,
            ) {
                viewModelScope.launch {
                    triggerSync()
                    startPeriodicSyncIfNeeded()
                }
            }
        }
        if (swap.leg2Status == PendingMultiSwap.STATUS_EXECUTING && !monitoringLeg2) {
            monitoringLeg2 = onChainMonitor.observeBalanceIncrease(
                coinUid = swap.coinUidOut,
                blockchainType = BlockchainType.fromUid(swap.blockchainTypeOut),
                scope = viewModelScope,
            ) {
                viewModelScope.launch {
                    triggerSync()
                    startPeriodicSyncIfNeeded()
                }
            }
        }
    }

    private suspend fun triggerSync() {
        if (!syncMutex.tryLock()) return
        try {
            syncPendingMultiSwapUseCase()
            val swap = pendingMultiSwapStorage.getById(pendingMultiSwapId)
            if (swap == null) {
                currentSwap = null
                closeScreen = true
                return
            }
            if (swap.leg1Status == PendingMultiSwap.STATUS_EXECUTING) monitoringLeg1 = false
            if (swap.leg2Status == PendingMultiSwap.STATUS_EXECUTING) monitoringLeg2 = false
            currentSwap = swap
            uiState = mapToUiState(swap)
            startMonitoringIfNeeded(swap)
        } finally {
            syncMutex.unlock()
        }
    }

    private fun hasExecutingLeg(): Boolean {
        val swap = currentSwap ?: return false
        return swap.leg1Status == PendingMultiSwap.STATUS_EXECUTING
            || swap.leg2Status == PendingMultiSwap.STATUS_EXECUTING
    }

    private fun startPeriodicSyncIfNeeded() {
        if (periodicSyncStarted) return
        if (!hasExecutingLeg()) return

        periodicSyncStarted = true
        viewModelScope.launch {
            while (isActive && hasExecutingLeg()) {
                triggerSync()
                delay(syncIntervalMs)
            }
            periodicSyncStarted = false
        }
    }

    private fun fiatAmount(coinUid: String, amount: BigDecimal?): BigDecimal? {
        if (amount == null) return null
        val rate = marketKit.coinPrice(coinUid, currency.code)?.value ?: return null
        return amount * rate
    }

    private fun resolveToken(coinUid: String, blockchainTypeUid: String): Token? {
        val blockchainType = BlockchainType.fromUid(blockchainTypeUid)
        return marketKit.fullCoins(listOf(coinUid))
            .firstOrNull()
            ?.tokens
            ?.firstOrNull { it.blockchainType == blockchainType }
    }

    private fun findProvider(providerId: String?): IMultiSwapProvider? {
        if (providerId == null) return null
        return swapQuoteService.findProviderById(providerId)
    }

    private fun needUseTimer(): Boolean {
        val provider = selectedLeg2Quote?.provider ?: return false
        return provider !is ChangeNowProvider && provider !is QuickexProvider
    }

    private fun startTimerIfNeeded() {
        if (currentSwap?.leg1Status == PendingMultiSwap.STATUS_COMPLETED && needUseTimer()) {
            timerService.start(QUOTE_LIFETIME)
        } else {
            timerService.reset()
        }
    }

    override fun onCleared() {
        timerService.stop()
    }

    private fun mapToUiState(swap: PendingMultiSwap): MultiSwapExchangeUiState {
        val tokenIn = resolveToken(swap.coinUidIn, swap.blockchainTypeIn)
        val tokenIntermediate = resolveToken(swap.coinUidIntermediate, swap.blockchainTypeIntermediate)
        val tokenOut = resolveToken(swap.coinUidOut, swap.blockchainTypeOut)

        val coinIn = marketKit.coin(swap.coinUidIn)
        val coinIntermediate = marketKit.coin(swap.coinUidIntermediate)
        val coinOut = marketKit.coin(swap.coinUidOut)

        val coinInCode = coinIn?.code ?: swap.coinUidIn
        val intermediateCoinCode = coinIntermediate?.code ?: swap.coinUidIntermediate
        val coinOutCode = coinOut?.code ?: swap.coinUidOut

        val leg1Status = mapStatus(swap.leg1Status)
        val leg2Status = mapStatus(swap.leg2Status)

        val hasQuotes = selectedLeg2Quote != null
        val buttonState = resolveButtonState(leg1Status, leg2Status, hasQuotes, timerState.timeout, quoting = leg2Quoting)
        val actionCreate = if (buttonState == ButtonState.Enabled) {
            if (tokenIntermediate != null && tokenOut != null) {
                selectedLeg2Quote?.provider?.getCreateTokenActionRequired(tokenIntermediate, tokenOut)
                    ?.let { ActionCreate(creatingWallets, it.descriptionResId, it.tokensToAdd) }
            } else null
        } else null
        val showContinueLater = leg2Status != LegStatus.Completed
            && leg1Status != LegStatus.Failed
            && leg2Status != LegStatus.Failed

        val leg1Provider = findProvider(swap.leg1ProviderId)

        // Leg2 provider: use selected quote's provider if available.
        // Before quotes are fetched, show stored provider as estimate.
        // After fetch completes with no quotes, show null → "No providers available".
        val leg2Provider = selectedLeg2Quote?.provider
            ?: if (!leg2QuoteFetched) findProvider(swap.leg2ProviderId) else null
        val fiatAmountIn = fiatAmount(swap.coinUidIn, swap.amountIn)
        val fiatAmountIntermediate = fiatAmount(swap.coinUidIntermediate, swap.leg1AmountOut)

        // Leg2 amounts: use quote data if available, otherwise stored/expected values
        val leg2AmountOut = selectedLeg2Quote?.amountOut ?: swap.leg2AmountOut ?: swap.expectedAmountOut
        val fiatAmountOut = fiatAmount(swap.coinUidOut, leg2AmountOut)

        return MultiSwapExchangeUiState(
            leg1 = LegUiState(
                status = leg1Status,
                providerName = leg1Provider?.title ?: swap.leg1ProviderId,
                providerIcon = leg1Provider?.icon,
                tokenIn = tokenIn,
                tokenOut = tokenIntermediate,
                amountIn = swap.amountIn,
                amountOut = swap.leg1AmountOut,
                fiatAmountIn = fiatAmountIn,
                fiatAmountOut = fiatAmountIntermediate,
                currency = currency,
                badgeIn = tokenIn?.badge,
                badgeOut = tokenIntermediate?.badge,
                coinIn = coinInCode,
                coinOut = intermediateCoinCode,
                coinIconUrlIn = coinImageUrl(swap.coinUidIn),
                coinIconUrlOut = coinImageUrl(swap.coinUidIntermediate),
                amountInFormatted = numberFormatter.formatCoinFull(swap.amountIn, null, 8),
                amountOutFormatted = swap.leg1AmountOut?.let { numberFormatter.formatCoinFull(it, null, 8) },
            ),
            leg2 = LegUiState(
                status = leg2Status,
                providerName = leg2Provider?.title,
                providerIcon = leg2Provider?.icon,
                tokenIn = tokenIntermediate,
                tokenOut = tokenOut,
                amountIn = swap.leg1AmountOut,
                amountOut = leg2AmountOut,
                fiatAmountIn = fiatAmountIntermediate,
                fiatAmountOut = fiatAmountOut,
                currency = currency,
                badgeIn = tokenIntermediate?.badge,
                badgeOut = tokenOut?.badge,
                coinIn = intermediateCoinCode,
                coinOut = coinOutCode,
                coinIconUrlIn = coinImageUrl(swap.coinUidIntermediate),
                coinIconUrlOut = coinImageUrl(swap.coinUidOut),
                amountInFormatted = swap.leg1AmountOut?.let { numberFormatter.formatCoinFull(it, null, 8) },
                amountOutFormatted = numberFormatter.formatCoinFull(leg2AmountOut, null, 8),
            ),
            buttonState = buttonState,
            showContinueLater = showContinueLater,
            leg1Clickable = (swap.leg1InfoRecordUid ?: swap.leg1TransactionId) != null,
            leg2ProviderClickable = leg1Status == LegStatus.Completed && leg2Status == LegStatus.Pending && hasQuotes,
            leg2Quoting = leg2Quoting,
            actionCreate = actionCreate,
        )
    }

    fun refreshQuotes() {
        currentSwap?.let { fetchLeg2Quotes(it) }
    }

    fun onContinueLater() {
        closeScreen = true
    }

    fun onDeleteAndClose() {
        viewModelScope.launch {
            pendingMultiSwapStorage.delete(pendingMultiSwapId)
            closeScreen = true
        }
    }

    fun createMissingWallets(tokens: Set<Token>) {
        if (creatingWallets) return
        creatingWallets = true
        currentSwap?.let { uiState = mapToUiState(it) }
        viewModelScope.launch {
            try {
                walletUseCase.createWallets(tokens)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create wallets")
            } finally {
                creatingWallets = false
                currentSwap?.let { uiState = mapToUiState(it) }
            }
        }
    }

    companion object {
        private const val SYNC_INTERVAL_MS = 10_000L
        private const val QUOTE_LIFETIME = 10L

        fun mapStatus(status: String): LegStatus = when (status) {
            PendingMultiSwap.STATUS_PENDING -> LegStatus.Pending
            PendingMultiSwap.STATUS_EXECUTING -> LegStatus.Executing
            PendingMultiSwap.STATUS_COMPLETED -> LegStatus.Completed
            PendingMultiSwap.STATUS_FAILED -> LegStatus.Failed
            else -> LegStatus.Pending
        }

        fun resolveButtonState(
            leg1: LegStatus,
            leg2: LegStatus,
            hasQuotes: Boolean = true,
            expired: Boolean = false,
            quoting: Boolean = false,
        ): ButtonState = when (leg1) {
            LegStatus.Completed if leg2 == LegStatus.Completed -> ButtonState.Close
            LegStatus.Completed if leg2 == LegStatus.Executing -> ButtonState.Hidden
            LegStatus.Completed if leg2 == LegStatus.Failed -> ButtonState.Hidden
            LegStatus.Completed if leg2 == LegStatus.Pending && quoting -> ButtonState.Quoting
            LegStatus.Completed if leg2 == LegStatus.Pending && hasQuotes && expired -> ButtonState.Refresh
            LegStatus.Completed if leg2 == LegStatus.Pending && hasQuotes -> ButtonState.Enabled
            LegStatus.Completed if leg2 == LegStatus.Pending && !hasQuotes -> ButtonState.Refresh
            else -> ButtonState.Disabled
        }
    }
}

data class MultiSwapExchangeUiState(
    val leg1: LegUiState,
    val leg2: LegUiState,
    val buttonState: ButtonState,
    val showContinueLater: Boolean,
    val leg1Clickable: Boolean = false,
    val leg2ProviderClickable: Boolean = false,
    val leg2Quoting: Boolean = false,
    val actionCreate: ActionCreate? = null,
)

data class LegUiState(
    val status: LegStatus,
    val providerName: String?,
    val providerIcon: Int? = null,
    val tokenIn: Token? = null,
    val tokenOut: Token? = null,
    val amountIn: BigDecimal? = null,
    val amountOut: BigDecimal? = null,
    val fiatAmountIn: BigDecimal? = null,
    val fiatAmountOut: BigDecimal? = null,
    val currency: Currency? = null,
    val badgeIn: String? = null,
    val badgeOut: String? = null,
    val coinIn: String,
    val coinOut: String,
    val coinIconUrlIn: String? = null,
    val coinIconUrlOut: String? = null,
    val amountInFormatted: String? = null,
    val amountOutFormatted: String? = null,
    val priceImpact: BigDecimal? = null,
    val priceImpactLevel: PriceImpactLevel? = null,
)

enum class LegStatus { Pending, Executing, Completed, Failed }
enum class ButtonState { Disabled, Enabled, Refresh, Close, Quoting, Hidden }
