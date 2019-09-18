package io.horizontalsystems.bankwallet.modules.pin.unlock

import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.factories.LockoutUntilDateFactory
import io.horizontalsystems.bankwallet.core.managers.CurrentDateProvider
import io.horizontalsystems.bankwallet.core.managers.LockoutManager
import io.horizontalsystems.bankwallet.core.managers.OneTimeTimer
import io.horizontalsystems.bankwallet.core.managers.UptimeProvider
import io.horizontalsystems.bankwallet.entities.LockoutState
import io.horizontalsystems.bankwallet.modules.pin.PinView

object UnlockPinModule {

    interface IRouter {
        fun dismissModuleWithSuccess()
    }

    interface IInteractor {
        val isFingerprintEnabled: Boolean
        val biometricAuthSupported: Boolean
        val cryptoObject: BiometricPrompt.CryptoObject?

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

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = PinView()
            val router = UnlockPinRouter()

            val lockoutManager = LockoutManager(App.localStorage, UptimeProvider(), LockoutUntilDateFactory(CurrentDateProvider()))
            val interactor = UnlockPinInteractor(App.pinManager, App.lockManager, lockoutManager, App.encryptionManager, App.systemInfoManager, OneTimeTimer())
            val presenter = UnlockPinPresenter(view, router, interactor, showCancelButton)

            interactor.delegate = presenter

            return presenter as T
        }
    }

}
