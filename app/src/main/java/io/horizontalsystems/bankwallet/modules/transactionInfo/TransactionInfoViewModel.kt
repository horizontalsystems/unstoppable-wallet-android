package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.transactions.TransactionRecordViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem

class TransactionInfoViewModel : ViewModel(), TransactionInfoModule.IView, TransactionInfoModule.IRouter {

    lateinit var delegate: TransactionInfoModule.IViewDelegate

    val transactionLiveData = SingleLiveEvent<TransactionViewItem>()
    val showDetailsLiveEvent = SingleLiveEvent<Pair<String, String>>()
    val closeLiveEvent = SingleLiveEvent<Unit>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()

    fun init(transactionHash: String) {
        TransactionInfoModule.init(this, this, transactionHash)
        delegate.viewDidLoad()
    }

    override fun showTransactionItem(transactionViewItem: TransactionViewItem) {
        transactionLiveData.value = transactionViewItem
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
