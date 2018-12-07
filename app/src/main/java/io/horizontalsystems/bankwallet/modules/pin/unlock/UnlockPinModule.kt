package io.horizontalsystems.bankwallet.modules.pin.unlock

import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.modules.pin.PinViewModel

object UnlockPinModule {
    interface IUnlockPinRouter {
        fun dismiss(didUnlock: Boolean)
    }

    interface IUnlockPinInteractor {
        fun cacheSecuredData()
        fun unlock(pin: String): Boolean
        fun onUnlock()
    }

    interface IUnlockPinInteractorDelegate {
        fun didBiometricUnlock()
        fun unlock()
        fun wrongPinSubmitted()
        fun showFingerprintInput(cryptoObject: FingerprintManagerCompat.CryptoObject)
    }

    fun init(view: PinViewModel, router: IUnlockPinRouter, keystoreSafeExecute: IKeyStoreSafeExecute) {

        val interactor = UnlockPinInteractor(keystoreSafeExecute, App.localStorage, App.wordsManager, App.pinManager, App.lockManager, App.encryptionManager)
        val presenter = UnlockPinPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}
