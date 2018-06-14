package bitcoin.wallet.modules.guest

class GuestPresenter : GuestModule.IPresenter, GuestModule.IInteractorDelegate {
    override lateinit var view: GuestModule.IView
    override lateinit var interactor: GuestModule.IInteractor
    override lateinit var router: GuestModule.IRouter

    override fun start() {
        // todo: do nothing?
    }

    override fun createWallet() {
        interactor.createWallet()
    }

    override fun restoreWallet() {
        router.openRestoreWalletScreen()
    }

    // interactor delegate

    override fun didCreateWallet() {
        router.openBackupScreen()
    }
}