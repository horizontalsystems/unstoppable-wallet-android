package io.horizontalsystems.bankwallet.modules.cryptonews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.xrateskit.entities.CryptoNews

object CryptoNewsModule {

    interface View {
        fun showSpinner()
        fun hideSpinner()
        fun showNews(data: List<CryptoNews>)
        fun showError(error: Throwable)
    }

    interface ViewDelegate {
        fun onLoad()
    }

    interface Interactor {
        fun getPosts(coinCode: String)
        fun clear()
    }

    interface InteractorDelegate {
        fun onReceivePosts(news: List<CryptoNews>)
        fun onError(error: Throwable)
    }

    interface Router

    class Factory(private val coinCode: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = CryptoNewsView()
            val interactor = CryptoNewsInteractor(App.xRateManager)
            val presenter = CryptoNewsPresenter(view, coinCode, interactor)

            interactor.delegate = presenter

            return presenter as T
        }
    }
}
