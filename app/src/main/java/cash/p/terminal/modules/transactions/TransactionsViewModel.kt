package cash.p.terminal.modules.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.MutableLiveData
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.managers.AmlStatusManager
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.core.storage.toRecordUidMap
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.premium.domain.PremiumSettings
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.nft.NftAssetBriefMetadata
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.ui_compose.ColoredValue
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.badge
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import io.horizontalsystems.core.ViewModelUiState
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.CurrencyValue
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.helpers.DateHelper
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.usecase.UpdateSwapProviderTransactionsStatusUseCase
import io.horizontalsystems.core.DispatcherProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class TransactionsViewModel(
    private val service: TransactionsService,
    private val transactionViewItem2Factory: TransactionViewItemFactory,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val transactionAdapterManager: TransactionAdapterManager,
    private val walletManager: IWalletManager,
    private val transactionFilterService: TransactionFilterService,
    private val transactionHiddenManager: TransactionHiddenManager,
    private val premiumSettings: PremiumSettings,
    private val amlStatusManager: AmlStatusManager,
    private val adapterManager: IAdapterManager,
    private val swapProviderTransactionsStorage: SwapProviderTransactionsStorage,
    private val contactsRepository: ContactsRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModelUiState<TransactionsUiState>() {

    var tmpItemToShow: TransactionItem? = null

    val filterResetEnabled = MutableLiveData<Boolean>()
    val filterTokensLiveData = MutableLiveData<List<Filter<FilterToken?>>>()
    val filterTypesLiveData = MutableLiveData<List<Filter<FilterTransactionType>>>()
    val filterBlockchainsLiveData = MutableLiveData<List<Filter<Blockchain?>>>()
    val filterContactLiveData = MutableLiveData<Contact?>()
    var filterHideSuspiciousTx = MutableLiveData<Boolean>()

    private var transactionListId: String? = null
    private var transactions: Map<String, List<TransactionViewItem>>? = null
    private var viewState: ViewState = ViewState.Loading
    private var syncing = service.syncingFlow.value
    private var hasHiddenTransactions: Boolean = false
    @Volatile private var filterVersion = 0
    @Volatile private var accountVersion = 0
    @Volatile private var cachedConvertedItems: List<TransactionViewItem> = emptyList()
    @Volatile private var awaitingAdaptersAfterSwitch = false
    private var currentFilterType: FilterTransactionType = FilterTransactionType.All
    private var amlPromoAlertEnabled = premiumSettings.getAmlCheckShowAlert()

    // Maps transaction record UID to SwapProviderTransaction for reactive updates
    private val swapStatusMap = MutableStateFlow(emptyMap<String, SwapProviderTransaction>())
    private val reprocessTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var swapObservationJob: Job? = null
    private var statusCheckerJob: Job? = null
    private val updateSwapProviderTransactionsStatusUseCase: UpdateSwapProviderTransactionsStatusUseCase = getKoinInstance()

    val balanceHidden: Boolean
        get() = balanceHiddenManager.balanceHidden

    fun toggleTransactionInfoHidden(transactionId: String) =
        balanceHiddenManager.toggleTransactionInfoHidden(transactionId)

    private var refreshViewItemsJob: Job? = null

    init {
        viewModelScope.launch {
            service.start()
        }

        viewModelScope.launch {
            transactionAdapterManager.adaptersReadyFlow.collect {
                withContext(dispatcherProvider.io) {
                    handleUpdatedWallets(walletManager.activeWallets)
                }
                // Don't clear awaitingAdaptersAfterSwitch here — partial batches from
                // AdapterManager would trigger service.set() → clear items → unguarded
                // empty flash. The flag is cleared when first non-empty data arrives
                // (handleUpdatedItems) or when initializationFlow signals all adapters
                // are ready (fallback for genuinely empty accounts).
                reprocessTrigger.tryEmit(Unit)
            }
        }

        viewModelScope.launch {
            transactionAdapterManager.initializationFlow.collect { initialized ->
                if (initialized && awaitingAdaptersAfterSwitch) {
                    awaitingAdaptersAfterSwitch = false
                    reprocessTrigger.tryEmit(Unit)
                }
            }
        }

        viewModelScope.launch {
            var currentAccountId: String? = null
            walletManager.activeWalletsFlow.collect { wallets ->
                val newAccountId = wallets.firstOrNull()?.account?.id
                if (newAccountId != null && newAccountId != currentAccountId) {
                    if (currentAccountId != null) {
                        accountVersion++
                        filterVersion++
                        cachedConvertedItems = emptyList()
                        viewState = ViewState.Loading
                        transactions = null
                        awaitingAdaptersAfterSwitch = true
                        service.cancelPendingLoads()
                        emitState()
                    }
                    currentAccountId = newAccountId
                    // Don't call handleUpdatedWallets here — TransactionAdapterManager
                    // still holds stale adapters. The adaptersReadyFlow collector will
                    // call handleUpdatedWallets once adapters are actually ready.
                }
            }
        }

        viewModelScope.launch {
            transactionFilterService.stateFlow.collect { state ->
                val transactionWallets = state.filterTokens.map { filterToken ->
                    filterToken?.let {
                        TransactionWallet(it.token, it.source, it.token.badge)
                    }
                }
                val selectedTransactionWallet = state.selectedToken?.let {
                    TransactionWallet(it.token, it.source, it.token.badge)
                }

                val accountId = walletManager.activeWallets.firstOrNull()?.account?.id.orEmpty()
                val newTransactionListId = accountId +
                        (selectedTransactionWallet?.hashCode() ?: 0).toString() +
                        state.selectedTransactionType.name +
                        state.selectedBlockchain?.uid

                // If filter changed, reset state to show loading and increment version
                if (transactionListId != newTransactionListId) {
                    transactionListId = newTransactionListId
                    filterVersion++
                    cachedConvertedItems = emptyList()
                    viewState = ViewState.Loading
                    transactions = null
                    emitState()
                }

                withContext(dispatcherProvider.io) {
                    service.set(
                        transactionWallets.filterNotNull(),
                        selectedTransactionWallet,
                        state.selectedTransactionType,
                        state.selectedBlockchain,
                        state.contact,
                    )
                }

                filterResetEnabled.postValue(state.resetEnabled)

                val types = state.transactionTypes
                val selectedType = state.selectedTransactionType
                currentFilterType = selectedType
                val filterTypes = types.map { Filter(it, it == selectedType) }
                filterTypesLiveData.postValue(filterTypes)

                val blockchains = state.blockchains
                val selectedBlockchain = state.selectedBlockchain
                val filterBlockchains = blockchains.map { Filter(it, it == selectedBlockchain) }
                filterBlockchainsLiveData.postValue(filterBlockchains)

                val filterCoins = state.filterTokens.map {
                    Filter(it, it == state.selectedToken)
                }
                filterTokensLiveData.postValue(filterCoins)

                filterContactLiveData.postValue(state.contact)

                if (filterHideSuspiciousTx.value != state.hideSuspiciousTx) {
                    service.reload()
                }
                filterHideSuspiciousTx.postValue(state.hideSuspiciousTx)

                // Observe swap transactions for selected token
                observeSwapsForToken(state.selectedToken)
            }
        }

        viewModelScope.launch {
            service.syncingFlow.collect {
                syncing = it
                emitState()
            }
        }

        viewModelScope.launch {
            combine(
                service.transactionItemsFlow,
                swapStatusMap,
                reprocessTrigger.onStart { emit(Unit) }
            ) { items, _, _ -> items }
                .collect { items ->
                    val distinct = items.distinctBy { it.record.uid }
                    handleUpdatedItems(distinct)
                }
        }

        viewModelScope.launch {
            balanceHiddenManager.balanceHiddenFlow.collect {
                withContext(dispatcherProvider.default) {
                    transactionViewItem2Factory.updateCache()
                }
                reprocessTrigger.tryEmit(Unit)
            }
        }

        viewModelScope.launch {
            balanceHiddenManager.anyTransactionVisibilityChangedFlow.collect {
                reprocessTrigger.tryEmit(Unit)
            }
        }

        viewModelScope.launch {
            contactsRepository.contactsFlow.collect {
                reprocessTrigger.tryEmit(Unit)
            }
        }

        viewModelScope.launch {
            transactionHiddenManager.transactionHiddenFlow.collectLatest {
                if (cachedConvertedItems.isNotEmpty()) {
                    applyHiddenFilter()
                }
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
    }

    fun showAllTransactions(show: Boolean) = transactionHiddenManager.showAllTransactions(show)

    private fun handleUpdatedItems(items: List<TransactionItem>) {
        // During account switch, skip empty emissions to prevent Loading → Success(empty) flash.
        // Both cancelPendingLoads and service.set() (triggered by each adapter batch) clear
        // service items, producing empty emissions. The flag stays true until either:
        // - first non-empty data arrives (below), or
        // - initializationFlow signals all adapters ready (fallback for empty accounts)
        if (items.isEmpty() && awaitingAdaptersAfterSwitch) {
            return
        }
        if (items.isNotEmpty() && awaitingAdaptersAfterSwitch) {
            awaitingAdaptersAfterSwitch = false
        }
        refreshViewItemsJob?.cancel()
        refreshViewItemsJob = viewModelScope.launch(dispatcherProvider.default) {
            val capturedVersion = accountVersion
            val capturedFilterVersion = filterVersion

            val allViewItems = items.map { item ->
                ensureActive()
                val matchedSwap = swapStatusMap.value[item.record.uid]
                transactionViewItem2Factory.convertToViewItemCached(
                    transactionItem = item,
                    matchedSwap = matchedSwap
                ).let { viewItem -> amlStatusManager.applyStatus(viewItem) }
            }

            ensureActive()

            // Discard if account or filter changed during async conversion
            if (capturedVersion == accountVersion && capturedFilterVersion == filterVersion) {
                cachedConvertedItems = allViewItems
                applyHiddenFilter()
            }
        }
    }

    private fun applyHiddenFilter() {
        val allViewItems = cachedConvertedItems
        val hiddenState = transactionHiddenManager.transactionHiddenFlow.value
        val filtered = if (hiddenState.transactionHidden) {
            when (hiddenState.transactionDisplayLevel) {
                TransactionDisplayLevel.NOTHING -> emptyList()
                TransactionDisplayLevel.LAST_1_TRANSACTION -> allViewItems.take(1)
                TransactionDisplayLevel.LAST_2_TRANSACTIONS -> allViewItems.take(2)
                TransactionDisplayLevel.LAST_4_TRANSACTIONS -> allViewItems.take(4)
            }.also { hasHiddenTransactions = allViewItems.size != it.size }
        } else {
            allViewItems.also { hasHiddenTransactions = false }
        }

        transactions = filtered.groupBy { it.formattedDate }
        viewState = ViewState.Success
        emitState()
    }

    private fun observeSwapsForToken(filterToken: FilterToken?) {
        swapObservationJob?.cancel()
        if (filterToken == null) {
            // Observe all swaps when no token is filtered
            swapObservationJob = viewModelScope.launch {
                swapProviderTransactionsStorage.observeAll().collect { swaps ->
                    swapStatusMap.value = swaps.toRecordUidMap()
                }
            }
            return
        }

        val wallet = walletManager.activeWallets.find {
            it.token == filterToken.token
        } ?: return

        val adapter = adapterManager.getReceiveAdapterForWallet(wallet) ?: return

        swapObservationJob = viewModelScope.launch {
            swapProviderTransactionsStorage.observeByToken(
                token = filterToken.token,
                address = adapter.receiveAddress
            ).collect { swaps ->
                swapStatusMap.value = swaps.toRecordUidMap()
            }
        }
    }

    private fun shouldShowAmlPromo(): Boolean {
        val hasTransactions = transactions?.values?.flatten()?.isNotEmpty() == true
        val isValidFilter = currentFilterType == FilterTransactionType.All ||
                currentFilterType == FilterTransactionType.Incoming
        return amlPromoAlertEnabled && hasTransactions && isValidFilter
    }

    override fun createState() = TransactionsUiState(
        transactions = transactions,
        viewState = viewState,
        transactionListId = transactionListId,
        syncing = syncing,
        hasHiddenTransactions = hasHiddenTransactions,
        showAmlPromo = shouldShowAmlPromo(),
        amlCheckEnabled = amlStatusManager.isEnabled
    )

    private fun handleUpdatedWallets(wallets: List<Wallet>) {
        transactionFilterService.setWallets(wallets)
    }

    fun setFilterTransactionType(filterType: FilterTransactionType) {
        transactionFilterService.setSelectedTransactionType(filterType)
    }

    fun setFilterToken(w: FilterToken?) {
        transactionFilterService.setSelectedToken(w)
    }

    fun onEnterFilterBlockchain(filterBlockchain: Filter<Blockchain?>) {
        transactionFilterService.setSelectedBlockchain(filterBlockchain.item)
    }

    fun onEnterContact(contact: Contact?) {
        transactionFilterService.setContact(contact)
    }

    fun resetFilters() {
        transactionFilterService.reset()
    }

    fun onBottomReached() {
        service.loadNext()
    }

    fun willShow(viewItem: TransactionViewItem) {
        service.fetchRateIfNeeded(viewItem.uid)
        fetchAmlStatusIfNeeded(viewItem.uid)
    }

    private fun fetchAmlStatusIfNeeded(uid: String) {
        val transactionItem = service.getTransactionItem(uid) ?: return
        amlStatusManager.fetchStatusIfNeeded(uid, transactionItem.record)
    }

    private fun updateTransactionAmlStatus(uid: String, status: AmlStatus?) {
        transactions?.let {
            transactions = it.withUpdatedAmlStatus(uid, status)
            emitState()
        }
    }

    fun startStatusChecker() {
        statusCheckerJob?.cancel()
        statusCheckerJob = viewModelScope.launch {
            while (isActive) {
                updateAllUnfinishedSwapStatuses()
                delay(30_000)
            }
        }
    }

    fun stopStatusChecker() {
        statusCheckerJob?.cancel()
    }

    private suspend fun updateAllUnfinishedSwapStatuses() {
        walletManager.activeWallets.forEach { wallet ->
            adapterManager.getReceiveAdapterForWallet(wallet)?.let { adapter ->
                updateSwapProviderTransactionsStatusUseCase(wallet.token, adapter.receiveAddress)
            }
        }
    }

    override fun onCleared() {
        service.clear()
    }

    fun getTransactionItem(viewItem: TransactionViewItem) =
        service.getTransactionItem(viewItem.uid)?.copy(
            transactionStatusUrl = viewItem.transactionStatusUrl,
            changeNowTransactionId = viewItem.changeNowTransactionId
        )

    fun updateFilterHideSuspiciousTx(checked: Boolean) {
        transactionFilterService.updateFilterHideSuspiciousTx(checked)
    }

    fun setAmlCheckEnabled(enabled: Boolean) {
        amlStatusManager.setEnabled(enabled)
    }

    fun dismissAmlPromo() {
        premiumSettings.setAmlCheckShowAlert(false)
        amlPromoAlertEnabled = false
        emitState()
    }

}

data class TransactionItem(
    val record: TransactionRecord,
    val currencyValue: CurrencyValue?,
    val lastBlockInfo: LastBlockInfo?,
    val nftMetadata: Map<NftUid, NftAssetBriefMetadata>,
    val changeNowTransactionId: String? = null,
    val transactionStatusUrl: Pair<String, String>? = null,
    val walletUid: String? = null
) {
    val createdAt = System.currentTimeMillis()
}

@Immutable
data class TransactionViewItem(
    val uid: String,
    val progress: Float?,
    val title: String,
    val subtitle: String,
    val primaryValue: ColoredValue?,
    val secondaryValue: ColoredValue?,
    val date: Date,
    val formattedTime: String,
    val showAmount: Boolean = true,
    val sentToSelf: Boolean = false,
    val doubleSpend: Boolean = false,
    val spam: Boolean = false,
    val locked: Boolean? = null,
    val icon: Icon,
    val changeNowTransactionId: String? = null,
    val transactionStatusUrl: Pair<String, String>? = null,
    val amlStatus: AmlStatus? = null
) {

    sealed class Icon {
        class ImageResource(val resourceId: Int) : Icon()
        class Regular(
            val url: String?,
            val alternativeUrl: String?,
            val placeholder: Int?,
            val rectangle: Boolean = false
        ) : Icon()

        class Double(val back: Regular, val front: Regular) : Icon()
        object Failed : Icon()
        class Platform(blockchainType: BlockchainType) : Icon() {
            val iconRes = when (blockchainType) {
                BlockchainType.BinanceSmartChain -> R.drawable.logo_chain_bsc_trx_24
                BlockchainType.Ethereum -> R.drawable.logo_chain_ethereum_trx_24
                BlockchainType.Polygon -> R.drawable.logo_chain_polygon_trx_24
                BlockchainType.Avalanche -> R.drawable.logo_chain_avalanche_trx_24
                BlockchainType.Optimism -> R.drawable.logo_chain_optimism_trx_24
                BlockchainType.Base -> R.drawable.logo_chain_base_trx_24
                BlockchainType.ZkSync -> R.drawable.logo_chain_zksync_trx_32
                BlockchainType.ArbitrumOne -> R.drawable.logo_chain_arbitrum_one_trx_24
                BlockchainType.Gnosis -> R.drawable.logo_chain_gnosis_trx_32
                BlockchainType.Fantom -> R.drawable.logo_chain_fantom_trx_32
                BlockchainType.Tron -> R.drawable.logo_chain_tron_trx_32
                BlockchainType.Ton -> R.drawable.logo_chain_ton_trx_32
                BlockchainType.Stellar -> R.drawable.logo_chain_stellar_trx_32
                else -> null
            }
        }
    }

    val formattedDate = formatDate(date).uppercase()

    private fun formatDate(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val today = Calendar.getInstance()
        if (calendar[Calendar.YEAR] == today[Calendar.YEAR] && calendar[Calendar.DAY_OF_YEAR] == today[Calendar.DAY_OF_YEAR]) {
            return Translator.getString(R.string.Timestamp_Today)
        }

        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        if (calendar[Calendar.YEAR] == yesterday[Calendar.YEAR] && calendar[Calendar.DAY_OF_YEAR] == yesterday[Calendar.DAY_OF_YEAR]) {
            return Translator.getString(R.string.Timestamp_Yesterday)
        }

        return DateHelper.shortDate(date, "MMMM d", "MMMM d, yyyy")
    }
}

enum class FilterTransactionType {
    All, Incoming, Outgoing, Swap, Approve;

    val title: Int
        get() = when (this) {
            All -> R.string.Transactions_All
            Incoming -> R.string.Transactions_Incoming
            Outgoing -> R.string.Transactions_Outgoing
            Swap -> R.string.Transactions_Swaps
            Approve -> R.string.Transactions_Approvals
        }
}

enum class AmlStatus {
    Loading,
    Unknown,
    Low,
    Medium,
    High;

    companion object {
        fun from(result: IncomingAddressCheckResult): AmlStatus = when (result) {
            IncomingAddressCheckResult.Unknown -> Unknown
            IncomingAddressCheckResult.Low -> Low
            IncomingAddressCheckResult.Medium -> Medium
            IncomingAddressCheckResult.High -> High
        }
    }
}

val AmlStatus.riskTextRes: Int
    get() = when (this) {
        AmlStatus.Low -> R.string.aml_low_risk
        AmlStatus.Medium -> R.string.aml_medium_risk
        AmlStatus.High -> R.string.aml_high_risk
        AmlStatus.Loading,
        AmlStatus.Unknown -> R.string.aml_unknown
    }

@Composable
fun AmlStatus.riskColor(): Color = when (this) {
    AmlStatus.Low -> ComposeAppTheme.colors.remus
    AmlStatus.Medium -> ComposeAppTheme.colors.jacob
    AmlStatus.High -> ComposeAppTheme.colors.lucian
    AmlStatus.Loading,
    AmlStatus.Unknown -> ComposeAppTheme.colors.grey50
}
