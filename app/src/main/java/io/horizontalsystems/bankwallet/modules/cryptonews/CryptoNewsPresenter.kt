package io.horizontalsystems.bankwallet.modules.cryptonews

import androidx.lifecycle.ViewModel
import io.horizontalsystems.xrateskit.entities.CryptoNews

class CryptoNewsPresenter(
        val view: CryptoNewsView,
        private val coinCode: String,
        private val interactor: CryptoNewsModule.Interactor)
    : CryptoNewsModule.ViewDelegate, CryptoNewsModule.InteractorDelegate, ViewModel() {

    //  ViewDelegate

    override fun onLoad() {
        view.showSpinner()
        interactor.getPosts(coinCode)
    }

    //  InteractorDelegate

    override fun onReceivePosts(news: List<CryptoNews>) {
        view.showNews(news)
        view.hideSpinner()
    }

    override fun onError(error: Throwable) {
        view.hideSpinner()
        view.showError(error)
    }

    //  ViewModel

    override fun onCleared() {
        interactor.clear()
    }
}