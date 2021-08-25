package io.horizontalsystems.bankwallet.modules.transactions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class TransactionsViewModel(
    private val service: TransactionsService,
    private val transactionViewItem2Factory: TransactionViewItemFactory
) : ViewModel() {

    lateinit var tmpItemToShow: TransactionItem

    val syncingLiveData = MutableLiveData<Boolean>()
    val filterCoinsLiveData = MutableLiveData<List<Filter<Wallet>>>()

    val filterTypes = FilterTransactionType.values()
    val selectedFilterLiveData = MutableLiveData(FilterTransactionType.All)

    val transactionList = MutableLiveData<ItemsList>()

    private val disposables = CompositeDisposable()

    init {
        service.syncingObservable
            .subscribeIO {
                syncingLiveData.postValue(it)
            }
            .let {
                disposables.add(it)
            }

        service.itemsObservable
            .subscribeIO {
                val transactionList = when {
                    it.isNotEmpty() -> {
                        ItemsList.Filled(it.map {
                            transactionViewItem2Factory.convertToViewItem(it)
                        })
                    }
                    else -> ItemsList.Blank
                }
                this.transactionList.postValue(transactionList)
            }
            .let {
                disposables.add(it)
            }

        Observable.combineLatest(
            service.filterCoinsObservable,
            service.filterCoinObservable
        ) { coins: List<Wallet>, selected: Optional<Wallet> ->
            coins.map { Filter(it, it == selected.orElse(null)) }
        }
            .subscribeIO {
                filterCoinsLiveData.postValue(it)
            }
            .let {
                disposables.add(it)
            }
    }

    fun setFilterTransactionType(f: FilterTransactionType) {
        TODO()
    }

    fun setFilterCoin(w: Wallet?) {
        service.setFilterCoin(w)
    }

    fun onBottomReached() {
        service.loadNext()
    }

    fun willShow(viewItem: TransactionViewItem) {
        service.fetchRateIfNeeded(viewItem.uid)
    }

    sealed class ItemsList(val items: List<TransactionViewItem>) {
        object Blank : ItemsList(listOf())
        class Filled(items: List<TransactionViewItem>) : ItemsList(items)
    }

    override fun onCleared() {
        service.clear()
    }

    fun getTransactionItem(viewItem: TransactionViewItem) = service.getTransactionItem(viewItem.uid)
}

data class TransactionItem(
    val record: TransactionRecord,
    val currencyValue: CurrencyValue?,
    val lastBlockInfo: LastBlockInfo?
)

data class TransactionViewItem(
    val uid: String,
    val typeIcon: Int,
    val progress: Int?,
    val title: String,
    val subtitle: String,
    val primaryValue: ColoredValue?,
    val secondaryValue: ColoredValue?,
    val date: Date,
    val sentToSelf: Boolean = false,
    val doubleSpend: Boolean = false,
    val locked: Boolean? = null
) {
    fun itemTheSame(newItem: TransactionViewItem) = uid == newItem.uid

    fun contentTheSame(newItem: TransactionViewItem): Boolean {
        return this == newItem
    }
}

enum class FilterTransactionType {
    All, Incoming, Outgoing, Swap, Approve
}
