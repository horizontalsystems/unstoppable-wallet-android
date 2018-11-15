package io.horizontalsystems.bankwallet.modules.pin.unlock

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
        fun biometricUnlock()
        fun onUnlock()
    }

    interface IUnlockPinInteractorDelegate {
        fun didBiometricUnlock()
        fun unlock()
        fun wrongPinSubmitted()
        fun showFingerprintInput()
    }

    fun init(view: PinViewModel, router: IUnlockPinRouter, keystoreSafeExecute: IKeyStoreSafeExecute) {

        val interactor = UnlockPinInteractor(keystoreSafeExecute, App.localStorage, App.wordsManager, App.pinManager, App.lockManager)
        val presenter = UnlockPinPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}
