package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Wallet

class FullTransactionInfoViewModel : ViewModel(), FullTransactionInfoModule.View, FullTransactionInfoModule.Router {

    lateinit var delegate: FullTransactionInfoModule.ViewDelegate

    val loadingLiveData = MutableLiveData<Boolean>()
    val reloadLiveEvent = SingleLiveEvent<Void>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()
    val showErrorProviderOffline = SingleLiveEvent<String>()
    val showErrorTransactionNotFound = SingleLiveEvent<String>()
    val hideError = SingleLiveEvent<Unit>()
    val showShareLiveEvent = SingleLiveEvent<String>()
    val openLinkLiveEvent = SingleLiveEvent<String>()
    val openProviderSettingsEvent = SingleLiveEvent<Pair<Coin, String>>()
    val shareButtonVisibility = MutableLiveData<Boolean>()

    fun init(transactionHash: String, wallet: Wallet) {
        FullTransactionInfoModule.init(this, this, wallet, transactionHash)
        delegate.viewDidLoad()
    }

    fun retry() {
        delegate.onRetryLoad()
    }

    fun share() {
        delegate.onShare()
    }

    fun changeProvider() {
        delegate.onTapChangeProvider()
    }

    //
    // IView
    //

    override fun setShareButtonVisibility(visible: Boolean) {
        shareButtonVisibility.postValue(visible)
    }

    override fun showLoading() {
        loadingLiveData.value = true
    }

    override fun hideLoading() {
        loadingLiveData.value = false
    }

    override fun showErrorProviderOffline(providerName: String) {
        showErrorProviderOffline.value = providerName
    }

    override fun showErrorTransactionNotFound(providerName: String) {
        showErrorTransactionNotFound.value = providerName
    }

    override fun hideError() {
        hideError.call()
    }

    override fun reload() {
        reloadLiveEvent.call()
    }

    override fun showCopied() {
        showCopiedLiveEvent.call()
    }

    override fun openProviderSettings(coin: Coin, transactionHash: String) {
        openProviderSettingsEvent.value = Pair(coin, transactionHash)
    }

    override fun openUrl(url: String) {
        openLinkLiveEvent.value = url
    }

    override fun share(url: String) {
        showShareLiveEvent.value = url
    }

    override fun onCleared() {
        delegate.onClear()
    }

}
