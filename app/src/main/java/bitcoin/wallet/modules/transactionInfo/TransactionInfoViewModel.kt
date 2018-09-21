package bitcoin.wallet.modules.transactionInfo

import android.arch.lifecycle.ViewModel
import bitcoin.wallet.SingleLiveEvent
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem

class TransactionInfoViewModel : ViewModel(), TransactionInfoModule.IView, TransactionInfoModule.IRouter {

    lateinit var delegate: TransactionInfoModule.IViewDelegate

    val transactionLiveData = SingleLiveEvent<TransactionRecordViewItem>()
    val showDetailsLiveEvent = SingleLiveEvent<Pair<String, String>>()
    val closeLiveEvent = SingleLiveEvent<Unit>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()

    fun init(transactionRecordViewItem: TransactionRecordViewItem) {
        TransactionInfoModule.init(this, this, transactionRecordViewItem)
        delegate.viewDidLoad()
    }

    override fun showTransactionItem(transactionRecordViewItem: TransactionRecordViewItem) {
        transactionLiveData.value = transactionRecordViewItem
    }

    override fun close() {
        closeLiveEvent.call()
    }

    override fun showCopied() {
        showCopiedLiveEvent.call()
    }

    override fun showFullInfo(transaction: TransactionRecordViewItem) {
        showDetailsLiveEvent.value = Pair(transaction.adapterId, transaction.hash)
    }
}
