package bitcoin.wallet.modules.transactions

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

class TransactionsViewModel : ViewModel(), TransactionsModule.IView, TransactionsModule.IRouter {

    lateinit var delegate: TransactionsModule.IViewDelegate

    val transactionItems = MutableLiveData<List<TransactionRecordViewItem>>()
    val showTransactionInfoLifeEvent = MutableLiveData<Pair<String, String>>()

    fun init() {
        TransactionsModule.initModule(this, this)
        delegate.viewDidLoad()
    }

    override fun showTransactionItems(items: List<TransactionRecordViewItem>) {
        transactionItems.value = items
    }

    override fun showTransactionInfo(coinCode: String, txHash: String) {
        showTransactionInfoLifeEvent.value = Pair(coinCode, txHash)
    }
}
