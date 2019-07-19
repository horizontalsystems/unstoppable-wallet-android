package io.horizontalsystems.bankwallet.modules.welcome

class WelcomePresenter(private val interactor: WelcomeModule.IInteractor, private val router: WelcomeModule.IRouter) : WelcomeModule.IViewDelegate, WelcomeModule.IInteractorDelegate {

    var view: WelcomeModule.IView? = null

    override fun viewDidLoad() {
        view?.setAppVersion(interactor.appVersion)
    }

    override fun createWalletDidClick() {
        interactor.createWallet()
    }

    override fun restoreWalletDidClick() {
        router.openRestoreModule()
    }

    // interactor delegate

    override fun didCreateWallet() {
        router.openMainModule()
    }

    override fun didFailToCreateWallet() {
        view?.showError()
    }

}
