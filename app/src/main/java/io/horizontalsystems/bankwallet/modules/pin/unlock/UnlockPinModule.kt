package io.horizontalsystems.bankwallet.modules.pin.unlock

import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.core.factories.LockoutUntilDateFactory
import io.horizontalsystems.bankwallet.core.managers.CurrentDateProvider
import io.horizontalsystems.bankwallet.core.managers.LockoutManager
import io.horizontalsystems.bankwallet.core.managers.OneTimeTimer
import io.horizontalsystems.bankwallet.core.managers.UptimeProvider
import io.horizontalsystems.bankwallet.entities.LockoutState
import io.horizontalsystems.bankwallet.modules.pin.PinViewModel

object UnlockPinModule {
    interface IUnlockPinRouter {
        fun dismiss(didUnlock: Boolean)
    }

    interface IUnlockPinInteractor {
        fun updateLockoutState()
        fun cacheSecuredData()
        fun unlock(pin: String): Boolean
        fun isBiometricOn(): Boolean
        fun onUnlock()
    }

    interface IUnlockPinInteractorDelegate {
        fun didBiometricUnlock()
        fun unlock()
        fun wrongPinSubmitted()
        fun setCryptoObject(cryptoObject: FingerprintManagerCompat.CryptoObject)
        fun updateLockoutState(state: LockoutState)
    }

    fun init(view: PinViewModel, router: IUnlockPinRouter, keystoreSafeExecute: IKeyStoreSafeExecute) {

        val lockoutManager = LockoutManager(App.localStorage, UptimeProvider(), LockoutUntilDateFactory(CurrentDateProvider()))
        val timer = OneTimeTimer()
        val interactor = UnlockPinInteractor(keystoreSafeExecute, App.localStorage, App.authManager, App.pinManager, App.lockManager, App.encryptionManager, lockoutManager, timer)
        val presenter = UnlockPinPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}
