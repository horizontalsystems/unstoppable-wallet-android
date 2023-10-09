package io.horizontalsystems.bankwallet.modules.transactions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.nft.NftAssetBriefMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class TransactionsViewModel(
    private val service: TransactionsService,
    private val transactionViewItem2Factory: TransactionViewItemFactory,
    private val balanceHiddenManager: BalanceHiddenManager
) : ViewModel() {

    var tmpItemToShow: TransactionItem? = null

    val syncingLiveData = MutableLiveData<Boolean>()
    val filterResetEnabled by service::filterResetEnabled
    val filterCoinsLiveData = MutableLiveData<List<Filter<TransactionWallet?>>>()
    val filterTypesLiveData = MutableLiveData<List<Filter<FilterTransactionType>>>()
    val filterBlockchainsLiveData = MutableLiveData<List<Filter<Blockchain?>>>()
    val transactionList = MutableLiveData<Map<String, List<TransactionViewItem>>>()
    val viewState = MutableLiveData<ViewState>(ViewState.Loading)

    private val disposables = CompositeDisposable()

    init {
        service.syncingObservable
            .subscribeIO {
                syncingLiveData.postValue(it)
            }
            .let {
                disposables.add(it)
            }

        service.typesObservable
            .subscribeIO { (types, selectedType) ->
                val filterTypes = types.map {
                    Filter(it, it == selectedType)
                }
                filterTypesLiveData.postValue(filterTypes)
            }
            .let {
                disposables.add(it)
            }

        service.blockchainObservable
            .subscribeIO { (blockchains, selectedType) ->
                val filterBlockchains = blockchains.map {
                    Filter(it, it == selectedType)
                }
                filterBlockchainsLiveData.postValue(filterBlockchains)
            }
            .let {
                disposables.add(it)
            }

        service.walletsObservable
            .subscribeIO { (wallets, selected) ->
                val filterCoins = wallets.map {
                    Filter(it, it == selected)
                }
                filterCoinsLiveData.postValue(filterCoins)
            }
            .let {
                disposables.add(it)
            }

        service.itemsObservable
            .subscribeIO { items ->
                val viewItems = items
                    .map { transactionViewItem2Factory.convertToViewItemCached(it) }
                    .groupBy { it.formattedDate }

                transactionList.postValue(viewItems)
                viewState.postValue(ViewState.Success)
            }
            .let {
                disposables.add(it)
            }

        viewModelScope.launch(Dispatchers.IO) {
            balanceHiddenManager.balanceHiddenFlow.collect {
                transactionViewItem2Factory.updateCache()
                service.refreshList()
            }
        }
    }

    fun setFilterTransactionType(filterType: FilterTransactionType) {
        service.setFilterType(filterType)
    }

    fun setFilterCoin(w: TransactionWallet?) {
        service.setFilterCoin(w)
    }

    fun onEnterFilterBlockchain(filterBlockchain: Filter<Blockchain?>) {
        service.setFilterBlockchain(filterBlockchain.item)
    }

    fun resetFilters() {
        service.resetFilters()
    }


    fun onBottomReached() {
        service.loadNext()
    }

    fun willShow(viewItem: TransactionViewItem) {
        service.fetchRateIfNeeded(viewItem.uid)
    }

    override fun onCleared() {
        service.clear()
        disposables.clear()
    }

    fun getTransactionItem(viewItem: TransactionViewItem) = service.getTransactionItem(viewItem.uid)
}

data class TransactionItem(
    val record: TransactionRecord,
    val currencyValue: CurrencyValue?,
    val lastBlockInfo: LastBlockInfo?,
    val nftMetadata: Map<NftUid, NftAssetBriefMetadata>
) {
    val createdAt = System.currentTimeMillis()
}

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
    val locked: Boolean? = null,
    val icon: Icon
) {

    sealed class Icon {
        class ImageResource(val resourceId: Int) : Icon()
        class Regular(val url: String?, val placeholder: Int?, val rectangle: Boolean = false) : Icon()
        class Double(val back: Regular, val front: Regular) : Icon()
        object Failed : Icon()
        class Platform(blockchainType: BlockchainType) : Icon() {
            val iconRes = when (blockchainType) {
                BlockchainType.BinanceSmartChain -> R.drawable.logo_chain_bsc_trx_24
                BlockchainType.Ethereum -> R.drawable.logo_chain_ethereum_trx_24
                BlockchainType.Polygon -> R.drawable.logo_chain_polygon_trx_24
                BlockchainType.Avalanche -> R.drawable.logo_chain_avalanche_trx_24
                BlockchainType.Optimism -> R.drawable.logo_chain_optimism_trx_24
                BlockchainType.ArbitrumOne -> R.drawable.logo_chain_arbitrum_one_trx_24
                BlockchainType.Gnosis -> R.drawable.logo_chain_gnosis_trx_32
                BlockchainType.Fantom -> R.drawable.logo_chain_fantom_trx_32
                BlockchainType.Tron -> R.drawable.logo_chain_tron_trx_32
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
