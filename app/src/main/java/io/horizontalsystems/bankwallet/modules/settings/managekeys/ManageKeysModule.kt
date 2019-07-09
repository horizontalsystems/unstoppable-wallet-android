package io.horizontalsystems.bankwallet.modules.settings.managekeys

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.SyncMode

object ManageKeysModule {

    interface View {
        fun show(items: List<ManageAccountItem>)
    }

    interface ViewDelegate {
        val items: List<ManageAccountItem>

        fun viewDidLoad()
        fun backupAccount(account: Account)
        fun restoreAccount(accountType: IPredefinedAccountType)
        fun unlinkAccount(id: String)
        fun onRestore(accountType: AccountType, syncMode: SyncMode)
        fun onClear()
    }

    interface Interactor {
        val predefinedAccountTypes: List<IPredefinedAccountType>
        fun account(predefinedAccountType: IPredefinedAccountType): Account?
        fun createAccount(defaultAccountType: DefaultAccountType)
        fun restoreAccount(accountType: AccountType, syncMode: SyncMode)

        fun loadAccounts()
        fun deleteAccount(id: String)
        fun clear()
    }

    interface InteractorDelegate {
        fun didLoad(accounts: List<ManageAccountItem>)
    }

    interface Router {
        fun startBackupModule(account: Account)
        fun startRestoreWords()
        fun close()
    }

    fun init(view: ManageKeysViewModel, router: Router) {
        val interactor = ManageKeysInteractor(App.accountManager, App.accountCreator, App.predefinedAccountTypeManager)
        val presenter = ManageKeysPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, ManageKeysActivity::class.java))
    }
}

data class ManageAccountItem(val predefinedAccountType: IPredefinedAccountType, val account: Account?)
