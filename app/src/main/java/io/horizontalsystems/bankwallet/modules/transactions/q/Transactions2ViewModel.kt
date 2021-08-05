package io.horizontalsystems.bankwallet.modules.transactions.q

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.ConfiguredCoin
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.TransactionLockState
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable

class Transactions2ViewModel(
    private val service: Transactions2Service,
    private val transactionViewItem2Factory: TransactionViewItem2Factory
) : ViewModel() {

    val syncingLiveData =
        service.syncingObservable.toFlowable(BackpressureStrategy.DROP).toLiveData()

    val filterTypes = FilterTransactionType.values()
    val selectedFilterLiveData = MutableLiveData(filterTypes.first())

    val filterCoinsLiveData = MutableLiveData<List<ConfiguredCoin>>()
    val selectedFilterCoinLiveData = MutableLiveData<Coin?>(null)

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
    }


    fun setFilterTransactionType(f: FilterTransactionType) {
        TODO()
    }

    fun setFilterCoin(f: ConfiguredCoin?) {
        TODO()
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
    fun itemTheSame(newItem: TransactionViewItem2): Boolean {
        return false
    }

    fun contentTheSame(newItem: TransactionViewItem2): Boolean {
        return false
    }
}

enum class FilterTransactionType {
    All, Incoming, Outgoing, Swap, Approve
}
