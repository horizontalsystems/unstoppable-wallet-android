package io.horizontalsystems.bankwallet.modules.transactions

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.SingleLiveEvent

class TransactionsViewModel : ViewModel(), TransactionsModule.IView, TransactionsModule.IRouter {

    lateinit var delegate: TransactionsModule.IViewDelegate

    val filterItems = MutableLiveData<List<Wallet?>>()
    val transactionViewItemLiveEvent = SingleLiveEvent<TransactionViewItem>()
    val items = MutableLiveData<List<TransactionViewItem>>()
    val reloadTransactions = SingleLiveEvent<Unit>()
    val showSyncing = SingleLiveEvent<Boolean>()

    private val mainThreadHandler = Handler(Looper.getMainLooper())

    fun init() {
        TransactionsModule.initModule(this, this)
        delegate.viewDidLoad()
    }

    override fun showFilters(filters: List<Wallet?>) {
        filterItems.postValue(filters)
    }

    override fun showTransactions(items: List<TransactionViewItem>) {
        mainThreadHandler.post {
            this.items.value = items
        }
    }

    override fun reloadTransactions() {
        reloadTransactions.postValue(Unit)
    }

    override fun showNoTransactions() {
        mainThreadHandler.post {
            items.value = listOf()
        }
    }

    override fun showSyncing() {
        showSyncing.postValue(true)
    }

    override fun hideSyncing() {
        showSyncing.postValue(false)
    }

    override fun openTransactionInfo(transactionViewItem: TransactionViewItem) {
        transactionViewItemLiveEvent.postValue(transactionViewItem)
    }

    override fun onCleared() {
        delegate.onClear()
    }
}
