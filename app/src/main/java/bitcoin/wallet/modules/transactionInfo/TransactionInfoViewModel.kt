package bitcoin.wallet.modules.transactionInfo

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.SingleLiveEvent
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem

class TransactionInfoViewModel : ViewModel(), TransactionInfoModule.IView, TransactionInfoModule.IRouter {

    lateinit var delegate: TransactionInfoModule.IViewDelegate

    val transactionLiveData = MutableLiveData<TransactionRecordViewItem>()
    val expandLiveEvent = SingleLiveEvent<Boolean>()

    fun init(coinCode: String, txHash: String) {
        TransactionInfoModule.init(this, this, coinCode, txHash)
        delegate.viewDidLoad()
    }

    override fun showTransactionItem(transactionRecordViewItem: TransactionRecordViewItem) {
        transactionLiveData.value = transactionRecordViewItem
    }

    override fun expand() {
        expandLiveEvent.value = true
    }

    override fun lessen() {
        expandLiveEvent.value = false
    }

}
