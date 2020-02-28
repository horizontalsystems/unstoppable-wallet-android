package io.horizontalsystems.bankwallet.modules.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object BaseModule {

    interface View {
        fun showTorConnectionStatus()
    }

    interface ViewDelegate {
        fun viewDidLoad()
    }

    interface Interactor {
        fun subscribeToEvents()
        fun clear()
    }

    interface InteractorDelegate {
        fun showTorConnectionStatus()
    }

    interface Router{

    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = BaseView()
            val router = BaseRouter()
            val interactor = BaseInteractor(App.netKitManager)
            val presenter = BasePresenter(view, router, interactor)

            interactor.delegate = presenter

            return presenter as T
        }
    }
}