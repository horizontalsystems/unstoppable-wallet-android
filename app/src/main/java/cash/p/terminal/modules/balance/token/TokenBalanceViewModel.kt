package cash.p.terminal.modules.balance.token

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.ILocalStorage
import io.horizontalsystems.core.IAppNumberFormatter
import cash.p.terminal.core.adapters.zcash.ZcashAdapter
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.isCustom
import cash.p.terminal.core.managers.AmlStatusManager
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.PriceManager
import cash.p.terminal.core.managers.StackingManager
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.core.storage.toRecordUidMap
import cash.p.terminal.core.swappable
import cash.p.terminal.core.tryOrNull
import cash.p.terminal.core.usecase.UpdateSwapProviderTransactionsStatusUseCase
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.featureStacking.ui.staking.StackingType
import cash.p.terminal.modules.balance.BackupRequiredError
import cash.p.terminal.modules.balance.BalanceViewItem
import cash.p.terminal.modules.balance.BalanceViewItemFactory
import cash.p.terminal.modules.balance.BalanceViewModel
import cash.p.terminal.modules.balance.TotalBalance
import cash.p.terminal.modules.balance.TotalService
import cash.p.terminal.modules.balance.token.TokenBalanceModule.StakingStatus
import cash.p.terminal.modules.balance.token.TokenBalanceModule.TokenBalanceUiState
import cash.p.terminal.modules.displayoptions.DisplayDiffOptionType
import cash.p.terminal.modules.displayoptions.DisplayPricePeriod
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.send.zcash.SendZCashViewModel
import cash.p.terminal.modules.transactions.AmlStatus
import cash.p.terminal.modules.transactions.TransactionItem
import cash.p.terminal.modules.transactions.TransactionViewItem
import cash.p.terminal.modules.transactions.TransactionViewItemFactory
import cash.p.terminal.modules.transactions.withClearedAmlStatus
import cash.p.terminal.modules.transactions.withUpdatedAmlStatus
import cash.p.terminal.network.pirate.domain.repository.PiratePlaceRepository
import cash.p.terminal.network.pirate.domain.useCase.GetChangeNowAssociatedCoinTickerUseCase
import cash.p.terminal.premium.domain.PremiumSettings
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IReceiveAdapter
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.badge
import cash.p.terminal.wallet.balance.BalanceItem
import cash.p.terminal.wallet.balance.BalanceViewType
import cash.p.terminal.wallet.balance.DeemedValue
import cash.p.terminal.wallet.isCosanta
import cash.p.terminal.wallet.isPirateCash
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import cash.p.terminal.wallet.tokenQueryId
import io.horizontalsystems.core.ViewModelUiState
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.logger.AppLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal

private const val ADAPTER_AWAIT_TIMEOUT_MS = 5000L

class TokenBalanceViewModel(
    private val totalBalance: TotalBalance,
    private val wallet: Wallet,
    private val balanceService: TokenBalanceService,
    private val balanceViewItemFactory: BalanceViewItemFactory,
    private val transactionsService: TokenTransactionsService,
    private val transactionViewItem2Factory: TransactionViewItemFactory,
    private val balanceHiddenManager: IBalanceHiddenManager,
    private val connectivityManager: ConnectivityManager,
    private val accountManager: IAccountManager,
    private val transactionHiddenManager: TransactionHiddenManager,
    private val getChangeNowAssociatedCoinTickerUseCase: GetChangeNowAssociatedCoinTickerUseCase,
    private val premiumSettings: PremiumSettings,
    private val amlStatusManager: AmlStatusManager,
    private val marketFavoritesManager: MarketFavoritesManager,
    private val piratePlaceRepository: PiratePlaceRepository,
    private val stackingManager: StackingManager,
    private val priceManager: PriceManager,
    private val localStorage: ILocalStorage,
    private val numberFormatter: IAppNumberFormatter,
    private val contactsRepository: ContactsRepository,
) : ViewModelUiState<TokenBalanceUiState>() {

    private val logger = AppLogger("TokenBalanceViewModel-${wallet.coin.code}")
    private val isStakingCoin = wallet.isPirateCash() || wallet.isCosanta()
    private val updateSwapProviderTransactionsStatusUseCase: UpdateSwapProviderTransactionsStatusUseCase =
        getKoinInstance()
    private val adapterManager: IAdapterManager = getKoinInstance()
    private val swapProviderTransactionsStorage: SwapProviderTransactionsStorage = getKoinInstance()

    private val title = wallet.token.coin.name

    private var balanceViewItem: BalanceViewItem? = null
    private var transactions: Map<String, List<TransactionViewItem>>? = null
    private var hasHiddenTransactions: Boolean = false
    private var amlPromoAlertEnabled = premiumSettings.getAmlCheckShowAlert()

    // Maps transaction record UID to SwapProviderTransaction for reactive updates
    private var swapStatusMap = emptyMap<String, SwapProviderTransaction>()

    private var statusCheckerJob: Job? = null
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    var secondaryValue by mutableStateOf(DeemedValue(""))
        private set

    var refreshing by mutableStateOf(false)
        private set

    private var showCurrencyAsSecondary = true
    private var isFavorite = marketFavoritesManager.isCoinInFavorites(wallet.coin.uid)
    private var stakingStatus: StakingStatus? = null
    private var stakingUnpaid: String? = null
    private var stakingChecked = false
    private var stakingCheckJob: Job? = null
    private var stakingAddress: String? = null

    private var displayDiffPricePeriod = localStorage.displayDiffPricePeriod
    private var displayDiffOptionType = localStorage.displayDiffOptionType
    private var isRoundingAmount = localStorage.isRoundingAmountMainPage
    private var hasReachedSynced = false

    init {
        viewModelScope.launch {
            balanceService.start()
            transactionsService.start()
            if (isStakingCoin) {
                stakingAddress = adapterManager.getReceiveAdapterForWallet(wallet)?.receiveAddress
                stakingAddress?.let { address ->
                    stackingManager.loadInvestmentData(
                        wallet = wallet,
                        address = address,
                        forceUpdate = true
                    )
                }
            }

            balanceService.balanceItemFlow.collect { balanceItem ->
                balanceItem?.let {
                    updateBalanceViewItem(
                        balanceItem = it,
                        isSwappable = isSwappable(it.wallet.token)
                    )
                    if (isStakingCoin) {
                        checkStakingStatus(it)
                    }
                }
            }
        }

        viewModelScope.launch {
            balanceHiddenManager.walletBalanceHiddenFlow(wallet.tokenQueryId).collect {
                balanceService.balanceItem?.let {
                    updateBalanceViewItem(
                        balanceItem = it,
                        isSwappable = isSwappable(it.wallet.token)
                    )
                    transactionViewItem2Factory.updateCache()
                    transactionsService.refreshList()
                }
            }
        }

        viewModelScope.launch {
            merge(
                priceManager.displayPricePeriodFlow.map {},
                priceManager.displayDiffOptionTypeFlow.map {},
            ).collect {
                val newPeriod = priceManager.displayPricePeriod
                val newOptionType = priceManager.displayDiffOptionType
                if (newPeriod == displayDiffPricePeriod && newOptionType == displayDiffOptionType) return@collect
                displayDiffPricePeriod = newPeriod
                displayDiffOptionType = newOptionType
                balanceService.balanceItem?.let {
                    updateBalanceViewItem(
                        balanceItem = it,
                        isSwappable = isSwappable(it.wallet.token)
                    )
                }
            }
        }

        viewModelScope.launch {
            transactionsService.transactionItemsFlow.collect {
                updateTransactions(it)
            }
        }

        viewModelScope.launch {
            transactionHiddenManager.transactionHiddenFlow.collectLatest {
                transactionsService.refreshList()
                refreshTransactionsFromCache()
            }
        }

        viewModelScope.launch {
            totalBalance.stateFlow.collectLatest { totalBalanceValue ->
                updateSecondaryValue(totalBalanceValue)
            }
        }

        viewModelScope.launch {
            balanceHiddenManager.anyTransactionVisibilityChangedFlow.collect {
                refreshTransactionsFromCache()
            }
        }

        viewModelScope.launch {
            contactsRepository.contactsFlow.collect {
                refreshTransactionsFromCache()
            }
        }

        viewModelScope.launch {
            amlStatusManager.statusUpdates.collect { update ->
                updateTransactionAmlStatus(update.uid, update.status)
            }
        }

        viewModelScope.launch {
            amlStatusManager.enabledStateFlow.collect { enabled ->
                if (enabled) {
                    // Trigger AML checks for currently loaded transactions
                    transactions?.values?.flatten()?.forEach { viewItem ->
                        fetchAmlStatusIfNeeded(viewItem.uid)
                    }
                } else {
                    // Remove AML status from all transactions
                    transactions = transactions?.withClearedAmlStatus()
                }
                emitState()
            }
        }

        viewModelScope.launch {
            val adapter = adapterManager.awaitAdapterForWallet<IReceiveAdapter>(
                wallet,
                ADAPTER_AWAIT_TIMEOUT_MS
            )
            if (adapter != null) {
                swapProviderTransactionsStorage.observeByToken(
                    token = wallet.token,
                    address = adapter.receiveAddress
                ).collect { swaps ->
                    swapStatusMap = swaps.toRecordUidMap()
                    refreshTransactionsFromCache()
                }
            }
        }

        totalBalance.start(viewModelScope)

        if (isStakingCoin) {
            viewModelScope.launch {
                stackingManager.unpaidFlow.collect { unpaid ->
                    stakingUnpaid = unpaid?.let { value ->
                        if (value > BigDecimal.ZERO) {
                            numberFormatter.formatCoinShort(value, wallet.coin.code, wallet.decimal)
                        } else null
                    }
                    emitState()
                }
            }
        }
    }

    private fun updateSecondaryValue(totalBalanceValue: TotalService.State = totalBalance.stateFlow.value) {
        val oldBalanceViewItem = balanceViewItem ?: return
        val fallbackValue = oldBalanceViewItem.secondaryValue.value

        val updatedValue = when (totalBalanceValue) {
            is TotalService.State.Visible -> {
                if (showCurrencyAsSecondary) {
                    totalBalanceValue.currencyValue?.getFormattedFull() ?: fallbackValue
                } else {
                    totalBalanceValue.coinValue?.getFormattedFull()
                        ?: secondaryValue.value.ifEmpty { fallbackValue }
                }
            }

            TotalService.State.Hidden -> fallbackValue
        }

        secondaryValue = oldBalanceViewItem.secondaryValue.copy(value = updatedValue)
    }

    private suspend fun isSwappable(token: Token) =
        App.instance.isSwapEnabled && (
                token.swappable ||
                        getChangeNowAssociatedCoinTickerUseCase(
                            token.coin.uid,
                            token.blockchainType.uid
                        ) != null)

    fun showAllTransactions(show: Boolean) = transactionHiddenManager.showAllTransactions(show)

    private fun refreshTransactionsFromCache() {
        val currentItems = transactionsService.transactionItemsFlow.value
        if (currentItems.isNotEmpty()) {
            updateTransactions(currentItems)
        }
    }

    fun startStatusChecker() {
        statusCheckerJob?.cancel()
        statusCheckerJob = viewModelScope.launch {
            while (isActive) {
                adapterManager.getReceiveAdapterForWallet(wallet)?.let { adapter ->
                    updateSwapProviderTransactionsStatusUseCase(
                        wallet.token,
                        adapter.receiveAddress
                    )
                }
                delay(30_000)
            }
        }
    }

    fun stopStatusChecker() {
        statusCheckerJob?.cancel()
    }

    private fun shouldShowAmlPromo(): Boolean {
        val hasTransactions = transactions?.values?.flatten()?.isNotEmpty() == true
        return amlPromoAlertEnabled && hasTransactions
    }

    private fun checkStakingStatus(balanceItem: BalanceItem) {
        val stackingType = if (wallet.isPirateCash()) StackingType.PCASH else StackingType.COSANTA
        val threshold = BigDecimal(stackingType.minStackingAmount)
        val balance = balanceItem.balanceData.total

        stakingStatus = if (balance >= threshold) StakingStatus.ACTIVE else StakingStatus.INACTIVE
        emitState()

        if (stakingStatus == StakingStatus.ACTIVE) return

        stakingCheckJob?.cancel()
        if (stakingChecked) return
        stakingChecked = true

        stakingCheckJob = viewModelScope.launch {
            try {
                val address = stakingAddress ?: return@launch
                val coinId =
                    if (wallet.isPirateCash()) {
                        StackingType.PCASH.value.lowercase()
                    } else {
                        StackingType.COSANTA.value.lowercase()
                    }
                val investmentData = piratePlaceRepository.getInvestmentData(coinId, address)
                val unrealized = tryOrNull { investmentData.unrealizedValue.toBigDecimal() }
                stakingStatus = if (unrealized != null && unrealized > BigDecimal.ZERO) {
                    StakingStatus.ACTIVE
                } else {
                    StakingStatus.INACTIVE
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking staking status")
                stakingStatus = StakingStatus.INACTIVE
            }
            emitState()
        }
    }

    override fun createState() = TokenBalanceUiState(
        title = title,
        coinCode = wallet.token.coin.code,
        badge = wallet.token.badge,
        balanceViewItem = balanceViewItem,
        transactions = transactions,
        hasHiddenTransactions = hasHiddenTransactions,
        showAmlPromo = shouldShowAmlPromo(),
        amlCheckEnabled = amlStatusManager.isEnabled,
        isFavorite = isFavorite,
        stakingStatus = stakingStatus,
        stakingUnpaid = stakingUnpaid,
        isCustomToken = wallet.token.isCustom,
        displayDiffPricePeriod = displayDiffPricePeriod,
        displayDiffOptionType = displayDiffOptionType,
        isRoundingAmount = isRoundingAmount,
        isShowShieldFunds = isShowShieldFunds()
    )

    private fun isShowShieldFunds(): Boolean {
        val item = balanceService.balanceItem ?: return hasReachedSynced
        val isTransparent =
            (item.wallet.token.type as? TokenType.AddressSpecTyped)?.type == TokenType.AddressSpecType.Transparent
        if (!isTransparent || item.balanceData.total <= ZcashAdapter.MINERS_FEE) return false

        if (item.state is AdapterState.Synced) {
            hasReachedSynced = true
        }
        return hasReachedSynced
    }

    private fun updateTransactions(items: List<TransactionItem>) {
        transactions =
            if (transactionHiddenManager.transactionHiddenFlow.value.transactionHidden) {
                when (transactionHiddenManager.transactionHiddenFlow.value.transactionDisplayLevel) {
                    TransactionDisplayLevel.NOTHING -> emptyList()
                    TransactionDisplayLevel.LAST_1_TRANSACTION -> items.take(1)
                    TransactionDisplayLevel.LAST_2_TRANSACTIONS -> items.take(2)
                    TransactionDisplayLevel.LAST_4_TRANSACTIONS -> items.take(4)
                }.also { hasHiddenTransactions = items.size != it.size }
            } else {
                items.also { hasHiddenTransactions = false }
            }.distinctBy { it.record.uid }
                .map { item ->
                    val matchedSwap = swapStatusMap[item.record.uid]
                    transactionViewItem2Factory.convertToViewItemCached(
                        transactionItem = item,
                        walletUid = wallet.tokenQueryId,
                        matchedSwap = matchedSwap
                    )
                }
                .map { amlStatusManager.applyStatus(it) }
                .groupBy { it.formattedDate }

        emitState()
    }

    private fun updateBalanceViewItem(balanceItem: BalanceItem, isSwappable: Boolean) {
        val balanceViewItem = balanceViewItemFactory.viewItem(
            item = balanceItem,
            currency = balanceService.baseCurrency,
            hideBalance = balanceHiddenManager.isWalletBalanceHidden(wallet.tokenQueryId),
            watchAccount = wallet.account.isWatchAccount,
            balanceViewType = BalanceViewType.CoinThenFiat,
            isSwappable = isSwappable,
            displayDiffOptionType = priceManager.displayDiffOptionType,
        )

        this.balanceViewItem = balanceViewItem.copy(
            primaryValue = balanceViewItem.primaryValue.copy(value = balanceViewItem.primaryValue.value + " " + balanceViewItem.wallet.coin.code)
        )

        totalBalance.setTotalServiceItems(
            listOf(
                TotalService.BalanceItem(
                    value = balanceItem.balanceData.total,
                    coinPrice = balanceItem.coinPrice,
                    isValuePending = false
                )
            )
        )

        updateSecondaryValue()
        emitState()
    }

    @Throws(BackupRequiredError::class, IllegalStateException::class)
    fun getWalletForReceive(): Wallet {
        val account =
            accountManager.activeAccount ?: throw IllegalStateException("Active account is not set")
        when {
            account.hasAnyBackup || !wallet.account.supportsBackup -> return wallet
            else -> throw BackupRequiredError(account, wallet.coin.name)
        }
    }

    fun onBottomReached() {
        transactionsService.loadNext()
    }

    fun willShow(viewItem: TransactionViewItem) {
        transactionsService.fetchRateIfNeeded(viewItem.uid)
        fetchAmlStatusIfNeeded(viewItem.uid)
    }

    private fun fetchAmlStatusIfNeeded(uid: String) {
        val transactionItem = transactionsService.getTransactionItem(uid) ?: return
        amlStatusManager.fetchStatusIfNeeded(uid, transactionItem.record)
    }

    private fun updateTransactionAmlStatus(uid: String, status: AmlStatus?) {
        transactions?.let {
            transactions = it.withUpdatedAmlStatus(uid, status)
            emitState()
        }
    }

    fun getTransactionItem(viewItem: TransactionViewItem) =
        transactionsService.getTransactionItem(viewItem.uid)?.copy(
            transactionStatusUrl = viewItem.transactionStatusUrl,
            changeNowTransactionId = viewItem.changeNowTransactionId,
            walletUid = wallet.tokenQueryId
        )

    fun toggleBalanceVisibility() {
        balanceHiddenManager.toggleWalletBalanceHidden(wallet.tokenQueryId)
    }

    fun toggleTotalType() {
        val currentSecondaryToken = totalBalance.stateFlow.value as? TotalService.State.Visible
        if (showCurrencyAsSecondary) {
            showCurrencyAsSecondary = false
            if (currentSecondaryToken?.coinValue?.coin?.uid == wallet.coin.uid) {
                totalBalance.toggleTotalType()
            } else {
                updateSecondaryValue()
            }
            return
        } else if (currentSecondaryToken?.coinValue?.coin?.uid == BlockchainType.Bitcoin.uid) {
            showCurrencyAsSecondary = true
            updateSecondaryValue()
        }
        totalBalance.toggleTotalType()
    }

    fun toggleFavorite() {
        val coinUid = wallet.coin.uid
        if (isFavorite) {
            marketFavoritesManager.remove(coinUid)
        } else {
            marketFavoritesManager.add(coinUid)
        }
        isFavorite = !isFavorite
        emitState()
    }

    fun getSyncErrorDetails(viewItem: BalanceViewItem): BalanceViewModel.SyncError = when {
        connectivityManager.isConnected.value -> BalanceViewModel.SyncError.Dialog(
            viewItem.wallet,
            viewItem.errorMessage
        )

        else -> BalanceViewModel.SyncError.NetworkNotAvailable()
    }

    fun proposeShielding() {
        val logger = logger.getScopedUnique()
        viewModelScope.launch {
            try {
                sendResult = SendResult.Sending
                (adapterManager.getAdapterForWalletOld(wallet) as? ZcashAdapter?)?.let { adapter ->
                    adapter.proposeShielding()
                }
                sendResult = SendResult.Sent()
            } catch (e: Throwable) {
                logger.warning("failed", e)
                sendResult = SendResult.Failed(SendZCashViewModel.createCaution(e))
            }
            delay(1000)
            sendResult = null
        }
    }

    fun refresh() = viewModelScope.launch {
        refreshing = true
        balanceService.refreshRates()

        adapterManager.refreshByWallet(wallet)
        delay(1000) // to show refresh indicator because `refreshByWallet` works asynchronously
        refreshing = false
    }

    override fun onCleared() {
        super.onCleared()
        balanceService.clear()
        totalBalance.stop()
    }

    fun setAmlCheckEnabled(enabled: Boolean) {
        amlStatusManager.setEnabled(enabled)
    }

    fun setDisplayPricePeriod(period: DisplayPricePeriod) {
        localStorage.displayDiffPricePeriod = period
    }

    fun setDisplayDiffOptionType(type: DisplayDiffOptionType) {
        localStorage.displayDiffOptionType = type
    }

    fun setRoundingAmount(enabled: Boolean) {
        localStorage.isRoundingAmountMainPage = enabled
        isRoundingAmount = enabled
        emitState()
    }

    fun dismissAmlPromo() {
        premiumSettings.setAmlCheckShowAlert(false)
        amlPromoAlertEnabled = false
        emitState()
    }
}
