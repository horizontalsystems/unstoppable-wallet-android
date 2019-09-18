package io.horizontalsystems.bankwallet.modules.pin.set

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.pin.PinInteractor
import io.horizontalsystems.bankwallet.modules.pin.PinView

object SetPinModule {

    interface IRouter {
        fun navigateToMain()
        fun dismissModuleWithSuccess()
    }

    class Factory : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = PinView()
            val router = SetPinRouter()

            val interactor = PinInteractor(App.pinManager)
            val presenter = SetPinPresenter(view, router, interactor)

            interactor.delegate = presenter

            return presenter as T
        }
    }

}
