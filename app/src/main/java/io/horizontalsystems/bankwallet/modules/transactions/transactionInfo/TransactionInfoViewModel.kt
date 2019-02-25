package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem

class TransactionInfoViewModel : ViewModel(), TransactionInfoModule.View, TransactionInfoModule.Router {

    lateinit var delegate: TransactionInfoModule.ViewDelegate

    val transactionLiveData = SingleLiveEvent<TransactionViewItem>()
    val showFullInfoLiveEvent = SingleLiveEvent<Pair<String, Coin>>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        TransactionInfoModule.init(this, this)
    }

    override fun showCopied() {
        showCopiedLiveEvent.call()
    }

    override fun openFullInfo(transactionHash: String, coin: Coin) {
        showFullInfoLiveEvent.value = Pair(transactionHash, coin)
    }

    fun setViewItem(transactionViewItem: TransactionViewItem) {
        transactionLiveData.value = transactionViewItem
    }

    fun onClickTransactionId() {
        transactionLiveData.value?.let {
            delegate.onCopy(it.transactionHash)
        }
    }

    fun onClickOpenFillInfo() {
        transactionLiveData.value?.let {
            delegate.openFullInfo(it.transactionHash, it.coin)
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
}
