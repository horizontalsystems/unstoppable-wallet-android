package cash.p.terminal.modules.transactions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import io.horizontalsystems.core.ViewModelUiState
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.managers.TransactionHiddenManager
import io.horizontalsystems.core.entities.CurrencyValue
import cash.p.terminal.entities.LastBlockInfo
import io.horizontalsystems.core.entities.ViewState
import cash.p.terminal.entities.nft.NftAssetBriefMetadata
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.ui_compose.ColoredValue
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.badge
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.util.Calendar
import java.util.Date

class TransactionsViewModel(
    private val service: TransactionsService,
    private val transactionViewItem2Factory: TransactionViewItemFactory,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val transactionAdapterManager: TransactionAdapterManager,
    private val walletManager: IWalletManager,
    private val transactionFilterService: TransactionFilterService,
    private val transactionHiddenManager: TransactionHiddenManager
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
    private var syncing = false
    private var hasHiddenTransactions: Boolean = false

    private var refreshViewItemsJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.Default) {
            service.start()
        }

        viewModelScope.launch(Dispatchers.Default) {
            transactionAdapterManager.adaptersReadyFlow.collect {
                handleUpdatedWallets(walletManager.activeWallets)
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
                    state.selectedTransactionType,
                    state.selectedBlockchain,
                    state.contact,
                )

                filterResetEnabled.postValue(state.resetEnabled)

                val types = state.transactionTypes
                val selectedType = state.selectedTransactionType
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

                if (filterHideSuspiciousTx.value != state.hideSuspiciousTx){
                    service.reload()
                }
                filterHideSuspiciousTx.postValue(state.hideSuspiciousTx)

                transactionListId = selectedTransactionWallet.hashCode().toString() +
                    state.selectedTransactionType.name +
                    state.selectedBlockchain?.uid
            }
        }

        viewModelScope.launch {
            service.syncingObservable.asFlow().collect {
                syncing = it
                emitState()
            }
        }

        viewModelScope.launch {
            service.itemsObservable.asFlow().collect { items ->
                handleUpdatedItems(items)
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            balanceHiddenManager.balanceHiddenFlow.collect {
                transactionViewItem2Factory.updateCache()
                service.refreshList()
            }
        }

        viewModelScope.launch {
            transactionHiddenManager.transactionHiddenFlow.collectLatest {
                service.reload()
            }
        }
    }

    fun showAllTransactions(show: Boolean) = transactionHiddenManager.showAllTransactions(show)

    private fun handleUpdatedItems(items: List<TransactionItem>) {
        refreshViewItemsJob?.cancel()
        refreshViewItemsJob = viewModelScope.launch(Dispatchers.Default) {
            val viewItems = if (transactionHiddenManager.transactionHiddenFlow.value.transactionHidden) {
                when (transactionHiddenManager.transactionHiddenFlow.value.transactionDisplayLevel) {
                    TransactionDisplayLevel.NOTHING -> emptyList()
                    TransactionDisplayLevel.LAST_1_TRANSACTION -> items.take(1)
                    TransactionDisplayLevel.LAST_2_TRANSACTIONS -> items.take(2)
                    TransactionDisplayLevel.LAST_4_TRANSACTIONS -> items.take(4)
                }.also { hasHiddenTransactions = items.size != it.size }
            } else {
                items.also { hasHiddenTransactions = false }
            }.map {
                    ensureActive()
                    transactionViewItem2Factory.convertToViewItemCached(it)
                }
                .groupBy {
                    ensureActive()
                    it.formattedDate
                }

            transactions = viewItems
            viewState = ViewState.Success

            ensureActive()
            emitState()
        }
    }

    override fun createState() = TransactionsUiState(
        transactions = transactions,
        viewState = viewState,
        transactionListId = transactionListId,
        syncing = syncing,
        hasHiddenTransactions = hasHiddenTransactions
    )

    private fun handleUpdatedWallets(wallets: List<cash.p.terminal.wallet.Wallet>) {
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
    }

    override fun onCleared() {
        service.clear()
    }

    fun getTransactionItem(viewItem: TransactionViewItem) = service.getTransactionItem(viewItem.uid)?.copy(
        transactionStatusUrl = viewItem.transactionStatusUrl,
        changeNowTransactionId = viewItem.changeNowTransactionId
    )

    fun updateFilterHideSuspiciousTx(checked: Boolean) {
        transactionFilterService.updateFilterHideSuspiciousTx(checked)
    }

}

data class TransactionItem(
    val record: TransactionRecord,
    val currencyValue: CurrencyValue?,
    val lastBlockInfo: LastBlockInfo?,
    val nftMetadata: Map<NftUid, NftAssetBriefMetadata>,
    val changeNowTransactionId: String? = null,
    val transactionStatusUrl: Pair<String, String>? = null
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
    val showAmount: Boolean = true,
    val sentToSelf: Boolean = false,
    val doubleSpend: Boolean = false,
    val spam: Boolean = false,
    val locked: Boolean? = null,
    val icon: Icon,
    val changeNowTransactionId: String? = null,
    val transactionStatusUrl: Pair<String, String>? = null
) {

    sealed class Icon {
        class ImageResource(val resourceId: Int) : Icon()
        class Regular(val url: String?, val alternativeUrl: String?, val placeholder: Int?, val rectangle: Boolean = false) : Icon()
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
                BlockchainType.ArbitrumOne -> R.drawable.logo_chain_arbitrum_one_trx_24
                BlockchainType.Gnosis -> R.drawable.logo_chain_gnosis_trx_32
                BlockchainType.Fantom -> R.drawable.logo_chain_fantom_trx_32
                BlockchainType.Tron -> R.drawable.logo_chain_tron_trx_32
                BlockchainType.Ton -> R.drawable.logo_chain_ton_trx_32
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
            return cash.p.terminal.strings.helpers.Translator.getString(R.string.Timestamp_Today)
        }

        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        if (calendar[Calendar.YEAR] == yesterday[Calendar.YEAR] && calendar[Calendar.DAY_OF_YEAR] == yesterday[Calendar.DAY_OF_YEAR]) {
            return cash.p.terminal.strings.helpers.Translator.getString(R.string.Timestamp_Yesterday)
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
