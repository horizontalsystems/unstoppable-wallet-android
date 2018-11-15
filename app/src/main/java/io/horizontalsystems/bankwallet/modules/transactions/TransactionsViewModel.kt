package io.horizontalsystems.bankwallet.modules.transactions

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class TransactionsViewModel : ViewModel(), TransactionsModule.IView, TransactionsModule.IRouter {

    lateinit var delegate: TransactionsModule.IViewDelegate

    val filterItems = MutableLiveData<List<TransactionFilterItem>>()
    val showTransactionInfoLiveEvent = SingleLiveEvent<TransactionRecordViewItem>()
    val didRefreshLiveEvent = SingleLiveEvent<Void>()
    val reloadLiveEvent = SingleLiveEvent<Void>()

    fun init() {
        TransactionsModule.initModule(this, this)
        delegate.viewDidLoad()
    }

    override fun showFilters(filters: List<TransactionFilterItem>) {
        filterItems.value = filters
    }

    override fun didRefresh() {
        didRefreshLiveEvent.call()
    }

    override fun reload() {
        reloadLiveEvent.call()
    }

    override fun openTransactionInfo(transactionHash: String) {
        showTransactionInfoLiveEvent.call()
    }

    override fun onCleared() {
        delegate.onClear()
    }
}
