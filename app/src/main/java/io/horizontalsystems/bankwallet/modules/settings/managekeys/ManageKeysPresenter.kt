package io.horizontalsystems.bankwallet.modules.settings.managekeys

import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.entities.*

class ManageKeysPresenter(private val interactor: ManageKeysModule.Interactor, private val router: ManageKeysModule.Router)
    : ManageKeysModule.ViewDelegate, ManageKeysModule.InteractorDelegate {

    var view: ManageKeysModule.View? = null

    //  ViewDelegate

    override var items = listOf<ManageAccountItem>()

    override fun viewDidLoad() {
        interactor.loadAccounts()
    }

    override fun onClickBackup(account: Account) {
        router.startBackupModule(account)
    }

    override fun onClickRestore(accountType: IPredefinedAccountType) {
        when (accountType) {
            is Words12AccountType -> {
                router.startRestoreWords()
            }
            is EosAccountType -> {
                router.startRestoreEos()
            }
        }
    }

    override fun onClickUnlink(accountId: String) {
        interactor.deleteAccount(accountId)
    }

    override fun onRestore(accountType: AccountType, syncMode: SyncMode?) {
        interactor.restoreAccount(accountType, syncMode)
    }

    override fun onClickNew(accountType: IPredefinedAccountType) {
        interactor.createAccount(accountType)
    }

    override fun onClear() {
        interactor.clear()
    }

    //  InteractorDelegate

    override fun didLoad(accounts: List<ManageAccountItem>) {
        items = accounts
        view?.show(items)
    }
}
