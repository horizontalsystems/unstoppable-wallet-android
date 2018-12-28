package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem

class TransactionInfoViewModel : ViewModel(), TransactionInfoModule.View, TransactionInfoModule.Router {

    lateinit var delegate: TransactionInfoModule.ViewDelegate

    val transactionLiveData = SingleLiveEvent<TransactionViewItem>()
    val showFullInfoLiveEvent = SingleLiveEvent<String>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        TransactionInfoModule.init(this, this)
    }

    override fun showTransactionItem(transactionViewItem: TransactionViewItem) {
        transactionLiveData.value = transactionViewItem
    }

    override fun showCopied() {
        showCopiedLiveEvent.call()
    }

    override fun showFullInfo(transactionHash: String) {
        showFullInfoLiveEvent.value = transactionHash
    }
}
