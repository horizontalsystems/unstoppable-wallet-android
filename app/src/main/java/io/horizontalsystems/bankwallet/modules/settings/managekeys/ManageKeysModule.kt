package io.horizontalsystems.bankwallet.modules.settings.managekeys

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.modules.settings.managekeys.views.ManageKeysActivity

object ManageKeysModule {

    interface View {
        fun show(items: List<ManageAccountItem>)
        fun showCreateConfirmation(accountItem: ManageAccountItem)
        fun showBackupConfirmation(accountItem: ManageAccountItem)
        fun showUnlinkConfirmation(accountItem: ManageAccountItem)
        fun showSuccess()
        fun showError(error: Exception)
    }

    interface ViewDelegate {
        val items: List<ManageAccountItem>

        fun viewDidLoad()
        fun onClickNew(accountItem: ManageAccountItem)
        fun onClickBackup(accountItem: ManageAccountItem)
        fun onClickRestore(accountType: IPredefinedAccountType)
        fun onClickUnlink(accountItem: ManageAccountItem)
        fun onClickShow(accountItem: ManageAccountItem)

        fun onConfirmCreate()
        fun onConfirmBackup()
        fun onConfirmUnlink(accountId: String)
        fun onConfirmRestore(accountType: AccountType, syncMode: SyncMode? = null)
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
        fun startBackupModule(accountItem: ManageAccountItem)
        fun startRestoreWords(wordsCount: Int, titleRes: Int)
        fun startRestoreEos(titleRes: Int)
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
