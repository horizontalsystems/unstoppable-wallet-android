package bitcoin.wallet.modules.guest

class GuestPresenter : GuestModule.IViewDelegate, GuestModule.IInteractorDelegate {
    lateinit var interactor: GuestModule.IInteractor
    lateinit var router: GuestModule.IRouter

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