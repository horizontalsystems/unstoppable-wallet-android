package io.horizontalsystems.bankwallet.modules.transactions.q

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.TransactionLockState
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class Transactions2ViewModel(
    private val service: Transactions2Service,
    private val transactionViewItem2Factory: TransactionViewItem2Factory
) : ViewModel() {

    val syncingLiveData =
        service.syncingObservable.toFlowable(BackpressureStrategy.DROP).toLiveData()
    val filterCoinsLiveData = MutableLiveData<List<Filter<Wallet>>>()

    val filterTypes = FilterTransactionType.values()
    val selectedFilterLiveData = MutableLiveData(FilterTransactionType.All)

    val transactionList = MutableLiveData<ItemsList>()

    private val disposables = CompositeDisposable()

    init {
        service.itemsObservable
            .subscribeIO {
                transactionList.postValue(ItemsList.Filled(it.map {
                    transactionViewItem2Factory.convertToViewItem(it)
                }))
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

    sealed class ItemsList {
        object Blank : ItemsList()
        class Filled(val items: List<TransactionViewItem2>) : ItemsList()
    }
}

data class TransactionItem(
    val record: TransactionRecord,
    val xxxCurrencyValue: CurrencyValue?,
    val lockState: TransactionLockState?
)

data class TransactionViewItem2(
    val uid: String,
    val typeIcon: Int,
    val progress: Int?,
    val title: String,
    val subtitle: String,
    val primaryValue: ColoredValue?,
    val secondaryValue: ColoredValue?,
    val locked: Boolean?,
    val sentToSelf: Boolean,
    val doubleSpend: Boolean
) {
    fun itemTheSame(newItem: TransactionViewItem2) = uid == newItem.uid

    fun contentTheSame(newItem: TransactionViewItem2): Boolean {
        return this == newItem
    }
}

enum class FilterTransactionType {
    All, Incoming, Outgoing, Swap, Approve
}
