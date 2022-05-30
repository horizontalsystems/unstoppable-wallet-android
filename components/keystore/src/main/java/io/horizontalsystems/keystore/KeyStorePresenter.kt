package io.horizontalsystems.keystore

import io.horizontalsystems.keystore.KeyStoreModule.ModeType

class KeyStorePresenter(
        private val interactor: KeyStoreModule.IInteractor,
        private val router: KeyStoreModule.IRouter,
        private val mode: ModeType)
    : KeyStoreModule.IViewDelegate, KeyStoreModule.IInteractorDelegate {

    var view: KeyStoreModule.IView? = null

    override fun viewDidLoad() {
        when (mode) {
            ModeType.NoSystemLock -> {
                interactor.resetApp("NoSystemLock")
                view?.showNoSystemLockWarning()
            }
            ModeType.InvalidKey -> {
                interactor.resetApp("InvalidKey")
                view?.showInvalidKeyWarning()
            }
            ModeType.UserAuthentication -> {
                view?.promptUserAuthentication()
            }
        }
    }

    override fun onCloseInvalidKeyWarning() {
        interactor.removeKey()
        router.openLaunchModule()
    }

    override fun onAuthenticationCanceled() {
        router.closeApplication()
    }

    override fun onAuthenticationSuccess() {
        router.openLaunchModule()
    }

}
