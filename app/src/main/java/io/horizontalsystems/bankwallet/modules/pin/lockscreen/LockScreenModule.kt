package io.horizontalsystems.bankwallet.modules.pin.lockscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

object LockScreenModule {

    interface ViewDelegate {
        fun onBackPressed()
    }

    interface Router{
        fun closeActivity()
        fun closeApplication()
    }

    class Factory(private val showCancelButton: Boolean) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val router = LockScreenRouter()
            val presenter = LockScreenPresenter(router, showCancelButton)

            return presenter as T
        }
    }
}
