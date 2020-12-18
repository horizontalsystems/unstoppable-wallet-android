package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Wallet

class FullTransactionInfoViewModel : ViewModel(), FullTransactionInfoModule.View, FullTransactionInfoModule.Router {

    lateinit var delegate: FullTransactionInfoModule.ViewDelegate

    val showLoadingEvent = SingleLiveEvent<Void>()
    val showTransactionInfoEvent = SingleLiveEvent<Void>()
    val reloadEvent = SingleLiveEvent<Void>()
    val showCopiedEvent = SingleLiveEvent<Unit>()
    val showErrorProviderOffline = SingleLiveEvent<Unit>()
    val showErrorTransactionNotFound = SingleLiveEvent<Unit>()
    val showShareEvent = SingleLiveEvent<String>()
    val openLinkEvent = SingleLiveEvent<String>()
    val openProviderSettingsEvent = SingleLiveEvent<Coin>()
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

    //
    // IView
    //

    override fun setShareButtonVisibility(visible: Boolean) {
        shareButtonVisibility.postValue(visible)
    }

    override fun showLoading() {
        showLoadingEvent.call()
    }

    override fun showErrorProviderOffline() {
        showErrorProviderOffline.postValue(Unit)
    }

    override fun showErrorTransactionNotFound() {
        showErrorTransactionNotFound.postValue(Unit)
    }

    override fun reload() {
        reloadEvent.call()
    }

    override fun showCopied() {
        showCopiedEvent.call()
    }

    override fun openProviderSettings(coin: Coin) {
        openProviderSettingsEvent.postValue(coin)
    }

    override fun openUrl(url: String) {
        openLinkEvent.value = url
    }

    override fun share(url: String) {
        showShareEvent.value = url
    }

    override fun onCleared() {
        delegate.onClear()
    }

    override fun showTransactionInfo() {
        showTransactionInfoEvent.call()
    }
}
