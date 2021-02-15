package io.horizontalsystems.bankwallet.modules.launcher

import io.horizontalsystems.core.security.KeyStoreValidationResult

class LaunchPresenter(private val interactor: LaunchModule.IInteractor,
                      private val router: LaunchModule.IRouter) : LaunchModule.IViewDelegate, LaunchModule.IInteractorDelegate {

    var view: LaunchModule.IView? = null

    // IViewDelegate methods

    override fun viewDidLoad() {
        if (interactor.isSystemLockOff){
            router.openNoSystemLockModule()
            return
        }

        when(interactor.validateKeyStore()) {
            KeyStoreValidationResult.UserNotAuthenticated -> {
                router.openUserAuthenticationModule()
                return
            }
            KeyStoreValidationResult.KeyIsInvalid -> {
                router.openKeyInvalidatedModule()
                return
            }
            KeyStoreValidationResult.KeyIsValid -> { /* Do nothing */}
        }

        when {
            !interactor.skipRootCheck && interactor.isDeviceRooted -> router.openDeviceIsRootedWarning()
            interactor.isAccountsEmpty && !interactor.mainShowedOnce -> router.openWelcomeModule()
            interactor.isPinNotSet -> router.openMainModule()
            else -> router.openUnlockModule()
        }
    }

    override fun didUnlock() {
        router.openMainModule()
    }

    override fun didCancelUnlock() {
        router.closeApplication()
    }

}
