package bitcoin.wallet.modules.transactions

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

class TransactionsViewModel : ViewModel(), TransactionsModule.IView, TransactionsModule.IRouter {

    lateinit var delegate: TransactionsModule.IViewDelegate

    val transactionItems = MutableLiveData<List<TransactionRecordViewItem>>()

    fun init() {
        TransactionsModule.initModule(this, this)
        delegate.viewDidLoad()
    }

    override fun showTransactionItems(items: List<TransactionRecordViewItem>) {
        transactionItems.value = items
    }
}
