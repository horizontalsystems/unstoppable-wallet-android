package io.horizontalsystems.bankwallet.modules.pin.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

object PinContainerModule {

    interface ViewDelegate {
        fun onBackPressed()
    }

    interface Router{
        fun closeActivity()
        fun closeApplication()
    }

    class Factory(private val showCancelButton: Boolean) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val router = PinContainerRouter()
            val presenter = PinContainerPresenter(router, showCancelButton)

            return presenter as T
        }
    }
}
