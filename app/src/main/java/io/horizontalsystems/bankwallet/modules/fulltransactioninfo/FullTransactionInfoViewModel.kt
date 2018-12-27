package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.transactions.TransactionRecordViewItem

class FullTransactionInfoViewModel: ViewModel(), FullTransactionInfoModule.IView, FullTransactionInfoModule.IRouter  {
    lateinit var delegate: FullTransactionInfoModule.IViewDelegate

    val showCopiedLiveEvent = SingleLiveEvent<Unit>()
    val showTransactionRecordViewLiveData = MutableLiveData<TransactionRecordViewItem>()
    val showBlockInfoLiveData = MutableLiveData<TransactionRecordViewItem>()
    val shareTransactionLiveData = MutableLiveData<TransactionRecordViewItem>()

    fun init(adapterId: String, transactionId: String) {
        FullTransactionInfoModule.init(this, this, adapterId, transactionId)
//        delegate.viewDidLoad()
    }

    override fun showTransactionItem(transactionRecordViewItem: TransactionRecordViewItem) {
        showTransactionRecordViewLiveData.value = transactionRecordViewItem
    }

    override fun showCopied() {
        showCopiedLiveEvent.call()
    }

    //IRouter methods
    override fun showBlockInfo(transaction: TransactionRecordViewItem) {
        showBlockInfoLiveData.value = transaction
    }

    override fun shareTransaction(transaction: TransactionRecordViewItem) {
        shareTransactionLiveData.value = transaction
    }
}
