package io.horizontalsystems.bankwallet.modules.launcher

class LaunchPresenter(private val interactor: LaunchModule.IInteractor,
                      private val router: LaunchModule.IRouter) : LaunchModule.IViewDelegate, LaunchModule.IInteractorDelegate {

    var view: LaunchModule.IView? = null

    // IViewDelegate methods

    override fun viewDidLoad() {
        when {
            interactor.isDeviceLockDisabled -> view?.showNoDeviceLockWarning()
            interactor.isAccountsEmpty -> router.openWelcomeModule()
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
