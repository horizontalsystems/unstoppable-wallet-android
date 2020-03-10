package io.horizontalsystems.bankwallet.modules.cryptonews

import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.xrateskit.entities.CryptoNews

class CryptoNewsView : CryptoNewsModule.View {
    val showNews = SingleLiveEvent<List<CryptoNews>>()
    val showError = SingleLiveEvent<Throwable>()
    val showSpinner = SingleLiveEvent<Boolean>()

    override fun showSpinner() {
        showSpinner.postValue(true)
    }

    override fun hideSpinner() {
        showSpinner.postValue(false)
    }

    override fun showNews(data: List<CryptoNews>) {
        showNews.postValue(data)
    }

    override fun showError(error: Throwable) {
        showError.postValue(error)
    }
}