package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class FullTransactionInfoViewModel : ViewModel(), FullTransactionInfoModule.View, FullTransactionInfoModule.Router {

    lateinit var delegate: FullTransactionInfoModule.ViewDelegate

    val loadingLiveData = MutableLiveData<Boolean>()
    val reloadLiveEvent = SingleLiveEvent<Void>()

    fun init(transactionHash: String, coinCode: CoinCode) {
        val transactionProvider = App.transactionInfoFactory.providerFor(coinCode)

        FullTransactionInfoModule.init(this, this, transactionProvider, transactionHash)
        delegate.viewDidLoad()
    }

    //
    // View
    //
    override fun show() {
    }

    override fun showLoading() {
        loadingLiveData.value = true
    }

    override fun hideLoading() {
        loadingLiveData.value = false
    }

    override fun reload() {
        reloadLiveEvent.call()
    }
}
