package io.horizontalsystems.bankwallet.modules.settings.managekeys

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.modules.settings.managekeys.views.ManageKeysActivity

object ManageKeysModule {

    interface View {
        fun show(items: List<ManageAccountItem>)
    }

    interface ViewDelegate {
        val items: List<ManageAccountItem>

        fun viewDidLoad()
        fun onClickBackup(account: Account)
        fun onClickRestore(accountType: IPredefinedAccountType)
        fun onClickUnlink(accountId: String)
        fun onClickNew(accountType: IPredefinedAccountType)
        fun onRestore(accountType: AccountType, syncMode: SyncMode? = null)
        fun onClear()
    }

    interface Interactor {
        val predefinedAccountTypes: List<IPredefinedAccountType>
        fun account(predefinedAccountType: IPredefinedAccountType): Account?
        fun createAccount(predefinedAccountType: IPredefinedAccountType)
        fun restoreAccount(accountType: AccountType, syncMode: SyncMode?)

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
        fun startRestoreEos()
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
