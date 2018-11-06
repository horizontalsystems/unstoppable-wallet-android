package bitcoin.wallet.modules.pin.unlock

import bitcoin.wallet.core.App
import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.modules.pin.PinViewModel

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

        val interactor = UnlockPinInteractor(keystoreSafeExecute, App.localStorage, App.wordsManager, App.adapterManager, App.pinManager, App.lockManager)
        val presenter = UnlockPinPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}
