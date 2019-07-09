package io.horizontalsystems.bankwallet.modules.settings.managekeys

import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.Words12AccountType

class ManageKeysPresenter(private val interactor: ManageKeysModule.Interactor, private val router: ManageKeysModule.Router)
    : ManageKeysModule.ViewDelegate, ManageKeysModule.InteractorDelegate {

    var view: ManageKeysModule.View? = null

    //  ViewDelegate

    override var items = listOf<ManageAccountItem>()

    override fun viewDidLoad() {
        interactor.loadAccounts()
    }

    override fun backupAccount(account: Account) {
        router.startBackupModule(account)
    }

    override fun restoreAccount(accountType: IPredefinedAccountType) {
        when (accountType) {
            is Words12AccountType -> {
                router.startRestoreWords()
            }
        }
    }

    override fun unlinkAccount(id: String) {
        interactor.deleteAccount(id)
    }

    override fun onRestore(accountType: AccountType, syncMode: SyncMode) {
        interactor.restoreAccount(accountType, syncMode)
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
