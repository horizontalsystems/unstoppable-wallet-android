package io.horizontalsystems.bankwallet.modules.settings.managekeys

import io.horizontalsystems.bankwallet.core.Account

class ManageKeysPresenter(private val interactor: ManageKeysModule.Interactor, private val router: ManageKeysModule.Router)
    : ManageKeysModule.ViewDelegate, ManageKeysModule.InteractorDelegate {

    var view: ManageKeysModule.View? = null

    //  ViewDelegate

    override var items = listOf<Account>()

    override fun viewDidLoad() {
        interactor.loadAccounts()
    }

    override fun backupAccount(account: Account) {
        interactor.backupAccount(account)
    }

    override fun unlinkAccount(id: String) {
        interactor.deleteAccount(id)
    }

    override fun onClear() {
        interactor.clear()
    }

    //  InteractorDelegate

    override fun didLoad(accounts: List<Account>) {
        items = accounts
        view?.show(items)
    }

    override fun accessIsRestricted() {
        router.showPinUnlock()
    }

    override fun openBackupWallet(account: Account) {
        router.showBackupWallet(account)
    }
}
