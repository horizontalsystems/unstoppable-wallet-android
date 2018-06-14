package bitcoin.wallet.modules.guest

class GuestPresenter(private val interactor: GuestModule.IInteractor, private val router: GuestModule.IRouter) : GuestModule.IViewDelegate, GuestModule.IInteractorDelegate {

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
}