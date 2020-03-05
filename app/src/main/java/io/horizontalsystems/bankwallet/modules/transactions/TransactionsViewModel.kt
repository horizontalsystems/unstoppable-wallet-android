package io.horizontalsystems.bankwallet.modules.transactions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.SingleLiveEvent

class TransactionsViewModel : ViewModel(), TransactionsModule.IView, TransactionsModule.IRouter {

    lateinit var delegate: TransactionsModule.IViewDelegate

    val filterItems = MutableLiveData<List<Wallet?>>()
    val transactionViewItemLiveEvent = SingleLiveEvent<TransactionViewItem>()
    val items = MutableLiveData<List<TransactionViewItem>>()

    fun init() {
        TransactionsModule.initModule(this, this)
        delegate.viewDidLoad()
    }

    override fun showFilters(filters: List<Wallet?>) {
        filterItems.postValue(filters)
    }

    override fun showTransactions(items: List<TransactionViewItem>) {
        this.items.postValue(items)
    }

    override fun addTransactions(items: List<TransactionViewItem>) {
        this.items.postValue(this.items.value ?: listOf<TransactionViewItem>() + items)
    }

    val noTransactions = MutableLiveData<Unit>()

    override fun showNoTransactions() {
        noTransactions.postValue(Unit)
    }

    override fun openTransactionInfo(transactionViewItem: TransactionViewItem) {
        transactionViewItemLiveEvent.postValue(transactionViewItem)
    }

    override fun onCleared() {
        delegate.onClear()
    }
}
