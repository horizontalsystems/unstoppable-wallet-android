package io.horizontalsystems.bankwallet.modules.settings.managekeys

class ManageKeysPresenter(
        private val interactor: ManageKeysModule.Interactor,
        private val router: ManageKeysModule.Router)
    : ManageKeysModule.ViewDelegate, ManageKeysModule.InteractorDelegate {

    var view: ManageKeysModule.View? = null

    private var currentItem: ManageAccountItem? = null

    //  IViewDelegate

    override var items = listOf<ManageAccountItem>()

    override fun viewDidLoad() {
        interactor.loadAccounts()
    }

    override fun onClickCreate(accountItem: ManageAccountItem) {
        currentItem = accountItem
        router.showCreateWallet(accountItem.predefinedAccountType)
    }

    override fun onClickBackup(accountItem: ManageAccountItem) {
        val account = accountItem.account ?: return
        router.showBackup(account, accountItem.predefinedAccountType)
    }

    override fun onClickRestore(accountItem: ManageAccountItem) {
        router.showCoinRestore(accountItem.predefinedAccountType)
    }

    override fun onClickUnlink(accountItem: ManageAccountItem) {
        currentItem = accountItem

        if (accountItem.account?.isBackedUp == true) {
            view?.showUnlinkConfirmation(accountItem)
        } else {
            view?.showBackupConfirmation(accountItem)
        }
    }

    override fun onConfirmBackup() {
        currentItem?.let {
            val account = it.account ?: return
            router.showBackup(account, it.predefinedAccountType)
        }
    }

    override fun onConfirmUnlink(accountId: String) {
        interactor.deleteAccount(accountId)
    }

    override fun onClear() {
        interactor.clear()
    }

    //  IInteractorDelegate

    override fun didLoad(accounts: List<ManageAccountItem>) {
        items = accounts
        view?.show(items)
    }

}
