package io.horizontalsystems.bankwallet.modules.welcome

class WelcomePresenter(
        private val interactor: WelcomeModule.IInteractor,
        private val router: WelcomeModule.IRouter) : WelcomeModule.IViewDelegate {

    var view: WelcomeModule.IView? = null

    override fun viewDidLoad() {
        view?.setAppVersion(interactor.appVersion)
    }

    override fun createWalletDidClick() {
        router.openCreateWalletModule()
    }

    override fun restoreWalletDidClick() {
        router.openRestoreModule()
    }

    override fun openTorPage() {
        router.openTorPage()
    }
}
