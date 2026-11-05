package io.horizontalsystems.walletkit.modules.transactions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.BuildConfig
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.core.managers.BalanceHiddenManager
import io.horizontalsystems.walletkit.core.managers.TransactionAdapterManager
import io.horizontalsystems.walletkit.core.managers.WalletManager
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.walletkit.entities.LastBlockInfo
import io.horizontalsystems.walletkit.entities.ViewState
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.helpers.DateHelper
import io.horizontalsystems.walletkit.modules.contacts.model.Contact
import io.horizontalsystems.walletkit.modules.transactionInfo.ColoredValue
import io.horizontalsystems.walletkit.uiv3.components.TransactionIconState
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class TransactionsViewModel(
    private val service: TransactionsService,
    private val transactionViewItem2Factory: TransactionViewItemFactory,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val transactionAdapterManager: TransactionAdapterManager,
    private val walletManager: WalletManager,
    private val transactionFilterService: TransactionFilterService,
    private val localStorage: ILocalStorage,
    private val swapRecordsRepository: SwapRecordsRepository,
) : ViewModelUiState<TransactionsUiState>() {


    val filterResetEnabled = MutableLiveData<Boolean>()
    val filterTokensLiveData = MutableLiveData<List<Filter<FilterToken?>>>()
    val filterBlockchainsLiveData = MutableLiveData<List<Filter<Blockchain?>>>()
    val filterContactLiveData = MutableLiveData<Contact?>()
    var filterHideSuspiciousTx = MutableLiveData<Boolean>()

    private var transactionListId: String? = null
    private var tab = ActivityTab.Transfers
    private var transactionItems: List<TransactionItem>? = null
    private var swapRecordItems: List<SwapRecordItem>? = null
    private var recordFilter: TransactionFilterService.State? = null
    private var transactions: Map<String, List<TransactionViewItem>>? = null
    // The tab [transactions] were built for, which trails [tab] until they are rebuilt.
    private var transactionsTab = tab
    private var viewState: ViewState = ViewState.Loading
    private var syncing = false

    private var refreshViewItemsJob: Job? = null

    init {
        transactionFilterService.observeSpamSetting(viewModelScope)

        viewModelScope.launch(Dispatchers.Default) {
            service.start()
        }

        swapRecordsRepository.start()

        viewModelScope.launch(Dispatchers.Default) {
            transactionAdapterManager.adaptersReadyFlow.collect {
                handleUpdatedWallets(walletManager.activeWallets)
                swapRecordsRepository.reload()
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            swapRecordsRepository.itemsFlow.collect {
                swapRecordItems = it
                refreshViewItems()
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            transactionFilterService.stateFlow.collect { state ->
                val transactionWallets = state.filterTokens.map { filterToken ->
                    filterToken?.let {
                        TransactionWallet(it.token, it.source, it.token.badge)
                    }
                }
                val selectedTransactionWallet = state.selectedToken?.let {
                    TransactionWallet(it.token, it.source, it.token.badge)
                }
                service.set(
                    transactionWallets.filterNotNull(),
                    selectedTransactionWallet,
                    FilterTransactionType.All,
                    state.selectedBlockchain,
                    state.contact,
                )

                filterResetEnabled.postValue(state.resetEnabled)
                recordFilter = state

                val blockchains = state.blockchains
                val selectedBlockchain = state.selectedBlockchain
                val filterBlockchains = blockchains.map { Filter(it, it == selectedBlockchain) }
                filterBlockchainsLiveData.postValue(filterBlockchains)

                val filterCoins = state.filterTokens.map {
                    Filter(it, it == state.selectedToken)
                }
                filterTokensLiveData.postValue(filterCoins)

                filterContactLiveData.postValue(state.contact)

                if (filterHideSuspiciousTx.value != state.hideSuspiciousTx){
                    service.reload()
                }
                filterHideSuspiciousTx.postValue(state.hideSuspiciousTx)

                transactionListId = selectedTransactionWallet.hashCode().toString() +
                    state.selectedBlockchain?.uid +
                    state.contact?.uid

                refreshViewItems()
            }
        }

        viewModelScope.launch {
            service.syncingFlow.collectSafely {
                syncing = it
                emitState()
            }
        }

        viewModelScope.launch {
            service.itemsFlow.collect { items ->
                transactionItems = items
                refreshViewItems()
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            balanceHiddenManager.balanceHiddenFlow.collect {
                transactionViewItem2Factory.updateCache()
                refreshViewItems()
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            localStorage.amountRoundingEnabledFlow.collect{
                service.reload()
            }
        }
    }

    // Called from the collectors' threads and the main thread.
    @Synchronized
    private fun refreshViewItems() {
        val tab = tab
        val items = transactionItems
        val records = swapRecordItems?.filter { tab.shows(it.kind) && matchesFilter(it) } ?: return
        if (tab == ActivityTab.Transfers && items == null) return

        refreshViewItemsJob?.cancel()
        refreshViewItemsJob = viewModelScope.launch(Dispatchers.Default) {
            val recordViewItems = records.map {
                ensureActive()
                transactionViewItem2Factory.convertToViewItem(it)
            }
            val viewItems = if (tab == ActivityTab.Transfers) {
                val transactionViewItems = items.orEmpty().map {
                    ensureActive()
                    transactionViewItem2Factory.convertToViewItemCached(it) to it.record.transactionHash
                }
                mergeActivity(
                    transactions = transactionViewItems,
                    swapRecords = recordViewItems,
                    depositHashes = records.mapNotNullTo(HashSet()) { it.record.transactionHash },
                    date = { it.date },
                )
            } else {
                recordViewItems
            }
            ensureActive()

            val grouped = viewItems.groupBy { it.formattedDate }
            val dummies = if (BuildConfig.DEBUG) DummyTransactions.viewItems(tab) else emptyList()
            transactions = if (dummies.isNotEmpty()) {
                mapOf(DummyTransactions.HEADER to dummies) + grouped
            } else {
                grouped
            }
            transactionsTab = tab
            viewState = ViewState.Success

            ensureActive()
            emitState()
        }
    }

    override fun createState() = TransactionsUiState(
        tab = transactionsTab,
        transactions = transactions,
        viewState = viewState,
        transactionListId = transactionListId + transactionsTab.name,
        syncing = syncing && transactionsTab == ActivityTab.Transfers,
    )

    // The filter's blockchain, coin and contact apply to swap records too: a record matches
    // when either of its sides does.
    private fun matchesFilter(item: SwapRecordItem): Boolean {
        val filter = recordFilter ?: return true
        val record = item.record

        filter.selectedToken?.let { filterToken ->
            val tokenId = filterToken.token.tokenQuery.id
            if (record.tokenInUid != tokenId && record.tokenOutUid != tokenId) return false
        }
        filter.selectedBlockchain?.let { blockchain ->
            val uid = blockchain.type.uid
            if (record.tokenInBlockchainTypeUid != uid && record.tokenOutBlockchainTypeUid != uid) return false
        }
        filter.contact?.let { contact ->
            val recipient = record.recipientAddress ?: return false
            if (contact.addresses.none { it.address.equals(recipient, ignoreCase = true) }) return false
        }
        return true
    }

    private fun handleUpdatedWallets(wallets: List<Wallet>) {
        transactionFilterService.setWallets(wallets)
    }

    fun setTab(tab: ActivityTab) {
        if (this.tab == tab) return
        this.tab = tab
        refreshViewItems()
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
    }

    override fun onCleared() {
        service.clear()
        swapRecordsRepository.clear()
    }

    fun getTransactionItem(viewItem: TransactionViewItem) = service.getTransactionItem(viewItem.uid)

    fun updateFilterHideSuspiciousTx(checked: Boolean) {
        transactionFilterService.updateFilterHideSuspiciousTx(checked)
    }

}

data class TransactionItem(
    val record: TransactionRecord,
    val currencyValue: CurrencyValue?,
    val lastBlockInfo: LastBlockInfo?
) {
    val createdAt = System.currentTimeMillis()
}

@Immutable
data class TransactionViewItem(
    val uid: String,
    val type: Type,
    val state: TransactionIconState,
    val title: Title,
    val subtitle: String,
    val primaryValue: ColoredValue?,
    val secondaryValue: ColoredValue?,
    val date: Date,
    val showAmount: Boolean = true,
    val doubleSpend: Boolean = false,
    val spam: Boolean = false,
    val locked: Boolean? = null,
    // Set when the row is a swap record rather than an on-chain transaction.
    val swapRecordId: Int? = null,
) {

    enum class Type(
        @DrawableRes val icon: Int,
        @StringRes val pendingText: Int,
        @StringRes val failedText: Int,
    ) {
        Receive(R.drawable.arrow_m_down_24, R.string.Transactions_Status_Receiving, R.string.Transactions_Status_TransferFailed),
        Send(R.drawable.arrow_m_up_24, R.string.Transactions_Status_Sending, R.string.Transactions_Status_TransferFailed),
        SendToSelf(R.drawable.arrow_return_24, R.string.Transactions_Status_Sending, R.string.Transactions_Status_TransferFailed),
        Approve(R.drawable.unlock_24, R.string.Transactions_Status_Approving, R.string.Transactions_Status_ApprovalFailed),
        Swap(R.drawable.swap_e_24, R.string.Transactions_Status_Swapping, R.string.Transactions_Status_SwapFailed),
        PrivateSwap(R.drawable.private_swap_24, R.string.Transactions_Status_Swapping, R.string.Transactions_Status_SwapFailed),
        PrivateSend(R.drawable.private_send_24, R.string.Transactions_Status_Sending, R.string.Transactions_Status_TransferFailed),
        CrossPay(R.drawable.double_arrow_24, R.string.Transactions_Status_Swapping, R.string.Transactions_Status_TransferFailed),
        Shield(R.drawable.shield_24, R.string.Transactions_Status_Shielding, R.string.Transactions_Status_ShieldingFailed),
        Unshield(R.drawable.shield_off_24, R.string.Transactions_Status_Unshielding, R.string.Transactions_Status_UnshieldFailed),
        Migrate(R.drawable.ic_migrate_24, R.string.Transactions_Status_Pending, R.string.Transactions_Status_TransactionFailed),
        Contract(R.drawable.list_24, R.string.Transactions_Status_Pending, R.string.Transactions_Status_TransactionFailed),
        Unknown(R.drawable.warning_24, R.string.Transactions_Status_Pending, R.string.Transactions_Status_TransactionFailed);

        // Null when completed: the row then shows what the transaction did instead of its state.
        @StringRes
        fun stateText(state: TransactionIconState): Int? = when (state) {
            TransactionIconState.Pending -> pendingText
            TransactionIconState.Failed -> failedText
            TransactionIconState.Completed -> null
        }
    }

    sealed class Title {
        data class Single(val text: String, val icon: Icon?) : Title()
        data class Pair(val from: Single, val to: Single) : Title()
    }

    sealed class Icon {
        class Regular(val url: String?, val alternativeUrl: String?, val placeholder: Int?, val rectangle: Boolean = false) : Icon()
        class Platform(val blockchainType: BlockchainType) : Icon() {
            val iconRes = when (blockchainType) {
                BlockchainType.BinanceSmartChain -> R.drawable.logo_chain_bsc_trx_24
                BlockchainType.Ethereum -> R.drawable.logo_chain_ethereum_trx_24
                BlockchainType.Polygon -> R.drawable.logo_chain_polygon_trx_24
                BlockchainType.Avalanche -> R.drawable.logo_chain_avalanche_trx_24
                BlockchainType.Optimism -> R.drawable.logo_chain_optimism_trx_24
                BlockchainType.Base -> R.drawable.logo_chain_base_trx_24
                BlockchainType.ZkSync -> R.drawable.logo_chain_zksync_trx_32
                BlockchainType.RobinhoodChain -> R.drawable.logo_chain_robinhood_trx_32
                BlockchainType.Arc -> R.drawable.logo_chain_arc_trx_32
                BlockchainType.ArbitrumOne -> R.drawable.logo_chain_arbitrum_one_trx_24
                BlockchainType.Gnosis -> R.drawable.logo_chain_gnosis_trx_32
                BlockchainType.Fantom -> R.drawable.logo_chain_fantom_trx_32
                BlockchainType.Tron -> R.drawable.logo_chain_tron_trx_32
                BlockchainType.Ton -> R.drawable.logo_chain_ton_trx_32
                BlockchainType.Stellar -> R.drawable.logo_chain_stellar_trx_32
                BlockchainType.Xrp -> R.drawable.logo_chain_xrp_trx_32
                BlockchainType.Near -> R.drawable.logo_chain_near_trx_32
                else -> null
            }
        }
    }

    val formattedDate = formatDate(date)

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

        return DateHelper.shortDate(date, "MMM d", "MMM d, yyyy")
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
