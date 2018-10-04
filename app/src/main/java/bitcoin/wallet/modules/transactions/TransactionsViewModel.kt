package bitcoin.wallet.modules.transactions

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.SingleLiveEvent

class TransactionsViewModel : ViewModel(), TransactionsModule.IView, TransactionsModule.IRouter {

    lateinit var delegate: TransactionsModule.IViewDelegate

    val transactionItems = MutableLiveData<List<TransactionRecordViewItem>>()
    val filterItems = MutableLiveData<List<TransactionFilterItem>>()
    val showTransactionInfoLiveEvent = SingleLiveEvent<TransactionRecordViewItem>()
    val didRefreshLiveEvent = SingleLiveEvent<Void>()

    fun init() {
        TransactionsModule.initModule(this, this)
        delegate.viewDidLoad()
    }

    override fun showTransactionItems(items: List<TransactionRecordViewItem>) {
        transactionItems.value = items
    }

    override fun showFilters(filters: List<TransactionFilterItem>) {
        filterItems.value = filters
    }

    override fun didRefresh() {
        didRefreshLiveEvent.call()
    }

    override fun showTransactionInfo(transaction: TransactionRecordViewItem) {
        showTransactionInfoLiveEvent.value = transaction
    }

    override fun onCleared() {
        delegate.onCleared()
        super.onCleared()
    }
}
