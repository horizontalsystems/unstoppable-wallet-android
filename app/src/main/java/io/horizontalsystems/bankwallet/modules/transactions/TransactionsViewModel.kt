package io.horizontalsystems.bankwallet.modules.transactions

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class TransactionsViewModel : ViewModel(), TransactionsModule.IView, TransactionsModule.IRouter {

    lateinit var delegate: TransactionsModule.IViewDelegate

    val filterItems = MutableLiveData<List<String?>>()
    val transactionViewItemLiveEvent = SingleLiveEvent<TransactionViewItem>()
    val didRefreshLiveEvent = SingleLiveEvent<Void>()
    val reloadLiveEvent = SingleLiveEvent<Pair<Int?, Int?>>()
    val reloadItemsLiveEvent = SingleLiveEvent<List<Int>>()

    fun init() {
        TransactionsModule.initModule(this, this)
        delegate.viewDidLoad()
    }

    override fun showFilters(filters: List<String?>) {
        filterItems.postValue(filters)
    }

    override fun reload(fromIndex: Int?, count: Int?) {
        reloadLiveEvent.postValue(Pair(fromIndex, count))
    }

    override fun reloadItems(updatedIndexes: List<Int>) {
        reloadItemsLiveEvent.postValue(updatedIndexes)
    }

    override fun openTransactionInfo(transactionViewItem: TransactionViewItem) {
        transactionViewItemLiveEvent.value = transactionViewItem
    }

    override fun onCleared() {
        delegate.onClear()
    }
}
