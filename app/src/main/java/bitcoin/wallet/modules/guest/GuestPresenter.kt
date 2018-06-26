package bitcoin.wallet.modules.guest

class GuestPresenter(private val interactor: GuestModule.IInteractor, private val router: GuestModule.IRouter) : GuestModule.IViewDelegate, GuestModule.IInteractorDelegate {

    var view: GuestModule.IView? = null

    override fun createWalletDidClick() {
        interactor.createWallet()
    }

    override fun restoreWalletDidClick() {
        router.navigateToRestore()
    }

    // interactor delegate

    override fun didCreateWallet() {
        router.navigateToBackupRoutingToMain()
    }

    override fun didFailToCreateWallet() {
        view?.showError()
    }

}
