package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.SingleLiveEvent
import java.util.*

class TransactionInfoViewModel : ViewModel(), TransactionInfoModule.View, TransactionInfoModule.Router {

    lateinit var delegate: TransactionInfoModule.ViewDelegate

    val titleLiveData = MutableLiveData<TransactionInfoModule.TitleViewItem>()
    val detailsLiveData = MutableLiveData<List<TransactionDetailViewItem>>()
    val showFullInfoLiveEvent = SingleLiveEvent<Pair<String, Wallet>>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()
    val showLockInfo = SingleLiveEvent<Date>()
    val showDoubleSpendInfo = SingleLiveEvent<Pair<String, String>>()
    val showShareLiveEvent = SingleLiveEvent<String>()

    fun init(transactionRecord: TransactionRecord, wallet: Wallet) {
        TransactionInfoModule.init(this, this, transactionRecord, wallet)
        delegate.viewDidLoad()
    }

    // IView

    override fun showCopied() {
        showCopiedLiveEvent.call()
    }

    override fun share(value: String) {
        showShareLiveEvent.value = value
    }

    override fun showTitle(titleViewItem: TransactionInfoModule.TitleViewItem) {
        titleLiveData.postValue(titleViewItem)
    }

    override fun showDetails(items: List<TransactionDetailViewItem>) {
        detailsLiveData.postValue(items)
    }

    // IRouter

    override fun openFullInfo(transactionHash: String, wallet: Wallet) {
        showFullInfoLiveEvent.value = Pair(transactionHash, wallet)
    }

    override fun openLockInfo(lockDate: Date) {
        showLockInfo.postValue(lockDate)
    }

    override fun openDoubleSpendInfo(transactionHash: String, conflictingTxHash: String) {
        showDoubleSpendInfo.postValue(Pair(transactionHash, conflictingTxHash))
    }

}
