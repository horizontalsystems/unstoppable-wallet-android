package io.horizontalsystems.bankwallet.modules.transactions

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.v7.util.DiffUtil
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin

class TransactionsViewModel : ViewModel(), TransactionsModule.IView, TransactionsModule.IRouter {

    lateinit var delegate: TransactionsModule.IViewDelegate

    val filterItems = MutableLiveData<List<Coin?>>()
    val transactionViewItemLiveEvent = SingleLiveEvent<TransactionViewItem>()
    val reloadChangeEvent = SingleLiveEvent<DiffUtil.DiffResult>()
    val initialLoadLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        TransactionsModule.initModule(this, this)
        delegate.viewDidLoad()
    }

    override fun initialLoad() {
        initialLoadLiveEvent.call()
    }

    override fun showFilters(filters: List<Coin?>) {
        filterItems.postValue(filters)
    }

    override fun reloadChange(diff: DiffUtil.DiffResult) {
        reloadChangeEvent.postValue(diff)
    }

    override fun openTransactionInfo(transactionViewItem: TransactionViewItem) {
        transactionViewItemLiveEvent.value = transactionViewItem
    }

    override fun onCleared() {
        delegate.onClear()
    }
}
