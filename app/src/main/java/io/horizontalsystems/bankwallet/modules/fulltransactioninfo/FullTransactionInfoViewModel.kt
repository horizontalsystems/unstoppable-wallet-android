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
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()
    val showErrorLiveEvent = SingleLiveEvent<Pair<Boolean, String?>>()
    val showShareLiveEvent = SingleLiveEvent<String>()
    val openLinkLiveEvent = SingleLiveEvent<String>()

    fun init(transactionHash: String, coinCode: CoinCode) {
        val transactionProvider = App.transactionInfoFactory.providerFor(coinCode)

        FullTransactionInfoModule.init(this, this, transactionProvider, transactionHash)
        delegate.viewDidLoad()
    }

    fun retry() {
        delegate.onRetryLoad()
    }

    fun share() {
        delegate.onShare()
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

    override fun showError(providerName: String) {
        showErrorLiveEvent.value = Pair(true, providerName)
    }

    override fun hideError() {
        showErrorLiveEvent.value = Pair(false, null)
    }

    override fun reload() {
        reloadLiveEvent.call()
    }

    override fun showCopied() {
        showCopiedLiveEvent.call()
    }

    override fun openUrl(url: String) {
        openLinkLiveEvent.value = url
    }

    override fun share(url: String) {
        showShareLiveEvent.value = url
    }
}
