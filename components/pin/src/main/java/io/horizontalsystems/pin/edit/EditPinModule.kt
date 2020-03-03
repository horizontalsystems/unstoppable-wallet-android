package io.horizontalsystems.pin.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.pin.PinInteractor
import io.horizontalsystems.pin.PinView

object EditPinModule {

    interface IRouter {
        fun dismissModuleWithSuccess()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = PinView()
            val router = EditPinRouter()

            val interactor = PinInteractor(CoreApp.pinComponent)
            val presenter = EditPinPresenter(view, router, interactor)

            interactor.delegate = presenter

            return presenter as T
        }
    }

}
