package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import java.util.*

class TransactionInfoViewModel : ViewModel(), TransactionInfoModule.View, TransactionInfoModule.Router {

    lateinit var delegate: TransactionInfoModule.ViewDelegate

    val transactionLiveData = SingleLiveEvent<TransactionViewItem>()
    val showFullInfoLiveEvent = SingleLiveEvent<Pair<String, Wallet>>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()
    val showLockInfo = SingleLiveEvent<Date>()
    val showDoubleSpendInfo = SingleLiveEvent<Pair<String, String>>()

    fun init() {
        TransactionInfoModule.init(this, this)
    }

    // IView

    override fun showCopied() {
        showCopiedLiveEvent.call()
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

    fun setViewItem(transactionViewItem: TransactionViewItem) {
        transactionLiveData.postValue(transactionViewItem)
    }

    fun onClickTransactionId() {
        transactionLiveData.value?.let {
            delegate.onCopy(it.transactionHash)
        }
    }

    fun onClickOpenFullInfo() {
        transactionLiveData.value?.let {
            delegate.openFullInfo(it.transactionHash, it.wallet)
        }
    }

    fun onClickFrom() {
        transactionLiveData.value?.from?.let {
            delegate.onCopy(it)
        }
    }

    fun onClickTo() {
        transactionLiveData.value?.to?.let {
            delegate.onCopy(it)
        }
    }

    fun onClickRecipientHash() {
        transactionLiveData.value?.lockInfo?.originalAddress?.let {
            delegate.onCopy(it)
        }
    }

    fun onClickLockInfo() {
        transactionLiveData.value?.lockInfo?.lockedUntil?.let {
            delegate.onClickLockInfo(it)
        }
    }

    fun onClickDoubleSpendInfo() {
        transactionLiveData.value?.let { tx ->
            if (tx.conflictingTxHash != null) {
                delegate.onClickDoubleSpendInfo(tx.transactionHash, tx.conflictingTxHash)
            }
        }
    }

}
