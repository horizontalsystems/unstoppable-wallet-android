package io.horizontalsystems.bankwallet.modules.transactions

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class TransactionsViewModel : ViewModel(), TransactionsModule.IView, TransactionsModule.IRouter {

    lateinit var delegate: TransactionsModule.IViewDelegate

    val filterItems = MutableLiveData<List<String?>>()
    val showTransactionInfoLiveEvent = SingleLiveEvent<String>()
    val didRefreshLiveEvent = SingleLiveEvent<Void>()
    val reloadLiveEvent = SingleLiveEvent<Pair<Int?, Int?>>()

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

    override fun openTransactionInfo(transactionHash: String) {
        showTransactionInfoLiveEvent.value = transactionHash
    }

    override fun onCleared() {
        delegate.onClear()
    }
}
