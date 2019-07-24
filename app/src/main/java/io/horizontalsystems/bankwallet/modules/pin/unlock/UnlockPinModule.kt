package io.horizontalsystems.bankwallet.modules.pin.unlock

import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.factories.LockoutUntilDateFactory
import io.horizontalsystems.bankwallet.core.managers.CurrentDateProvider
import io.horizontalsystems.bankwallet.core.managers.LockoutManager
import io.horizontalsystems.bankwallet.core.managers.OneTimeTimer
import io.horizontalsystems.bankwallet.core.managers.UptimeProvider
import io.horizontalsystems.bankwallet.entities.LockoutState
import io.horizontalsystems.bankwallet.modules.pin.PinViewModel

object UnlockPinModule {
    interface IUnlockPinRouter {
        fun dismissModuleWithSuccess()
        fun dismissModuleWithCancel()
        fun closeApplication()
    }

    interface IUnlockPinInteractor {
        fun updateLockoutState()
        fun unlock(pin: String): Boolean
        fun isBiometricOn(): Boolean
        fun onUnlock()
    }

    interface IUnlockPinInteractorDelegate {
        fun unlock()
        fun wrongPinSubmitted()
        fun setCryptoObject(cryptoObject: FingerprintManagerCompat.CryptoObject)
        fun updateLockoutState(state: LockoutState)
    }

    fun init(view: PinViewModel, router: IUnlockPinRouter, showCancelButton: Boolean) {

        val lockoutManager = LockoutManager(App.localStorage, UptimeProvider(), LockoutUntilDateFactory(CurrentDateProvider()))
        val timer = OneTimeTimer()
        val interactor = UnlockPinInteractor(App.localStorage, App.pinManager, App.lockManager, lockoutManager, timer)
        val presenter = UnlockPinPresenter(interactor, router, showCancelButton)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
