package io.horizontalsystems.bankwallet.modules.settings.managekeys

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.settings.managekeys.views.ManageKeysActivity

object ManageKeysModule {

    interface View {
        fun show(items: List<ManageAccountItem>)
        fun showBackupConfirmation(accountItem: ManageAccountItem)
        fun showUnlinkConfirmation(accountItem: ManageAccountItem)
    }

    interface ViewDelegate {
        val items: List<ManageAccountItem>

        fun viewDidLoad()
        fun onClickCreate(accountItem: ManageAccountItem)
        fun onClickBackup(accountItem: ManageAccountItem)
        fun onClickRestore(accountItem: ManageAccountItem)
        fun onClickUnlink(accountItem: ManageAccountItem)

        fun onConfirmBackup()
        fun onConfirmUnlink(accountId: String)
        fun onClear()
    }

    interface Interactor {
        val predefinedAccountTypes: List<PredefinedAccountType>
        fun account(predefinedAccountType: PredefinedAccountType): Account?

        fun loadAccounts()
        fun deleteAccount(id: String)
        fun clear()
    }

    interface InteractorDelegate {
        fun didLoad(accounts: List<ManageAccountItem>)
    }

    interface Router {
        fun close()
        fun showCreateWallet(predefinedAccountType: PredefinedAccountType)
        fun showBackup(account: Account, predefinedAccountType: PredefinedAccountType)
        fun showCoinRestore(predefinedAccountType: PredefinedAccountType)
    }

    fun init(view: ManageKeysViewModel, router: Router) {
        val interactor = ManageKeysInteractor(App.accountManager, App.predefinedAccountTypeManager)
        val presenter = ManageKeysPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, ManageKeysActivity::class.java))
    }
}

data class ManageAccountItem(val predefinedAccountType: PredefinedAccountType, val account: Account?)
