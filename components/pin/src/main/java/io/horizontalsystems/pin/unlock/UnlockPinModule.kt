package io.horizontalsystems.pin.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.CurrentDateProvider
import io.horizontalsystems.pin.PinView
import io.horizontalsystems.pin.core.*

object UnlockPinModule {

    interface IRouter {
        fun dismissModuleWithSuccess()
    }

    interface IInteractor {
        val isBiometricAuthEnabled: Boolean
        val isBiometricAuthSupported: Boolean

        fun updateLockoutState()
        fun unlock(pin: String): Boolean
        fun onUnlock()
    }

    interface IInteractorDelegate {
        fun unlock()
        fun wrongPinSubmitted()
        fun updateLockoutState(state: LockoutState)
    }

    class Factory(private val showCancelButton: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = PinView()
            val router = UnlockPinRouter()

            val lockoutManager = LockoutManager(CoreApp.pinStorage, UptimeProvider(), LockoutUntilDateFactory(CurrentDateProvider()))
            val interactor = UnlockPinInteractor(CoreApp.pinComponent, lockoutManager, CoreApp.systemInfoManager, OneTimeTimer())
            val presenter = UnlockPinPresenter(view, router, interactor, showCancelButton)

            interactor.delegate = presenter

            return presenter as T
        }
    }

}
