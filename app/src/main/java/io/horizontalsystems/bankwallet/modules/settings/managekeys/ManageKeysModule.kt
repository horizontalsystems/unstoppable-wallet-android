package io.horizontalsystems.bankwallet.modules.settings.managekeys

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.settings.managekeys.views.ManageKeysActivity

object ManageKeysModule {

    interface IView {
        fun show(items: List<ManageAccountItem>)
        fun showBackupConfirmation(accountItem: ManageAccountItem)
        fun showUnlinkConfirmation(accountItem: ManageAccountItem)
    }

    interface ViewDelegate {
        fun onLoad()
        fun onClickCreate(accountItem: ManageAccountItem)
        fun onClickBackup(accountItem: ManageAccountItem)
        fun onClickRestore(accountItem: ManageAccountItem)
        fun onClickUnlink(accountItem: ManageAccountItem)

        fun onConfirmBackup()
        fun onConfirmUnlink(accountId: String)
        fun onClear()
        fun didEnterValidAccount(accountType: AccountType)
        fun didReturnFromCoinSettings()
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

    interface IRouter {
        fun close()
        fun showCoinSettings()
        fun showCreateWallet(predefinedAccountType: PredefinedAccountType)
        fun showBackup(account: Account, predefinedAccountType: PredefinedAccountType)
        fun showCoinManager(predefinedAccountType: PredefinedAccountType, accountType: AccountType)
        fun showRestoreKeyInput(predefinedAccountType: PredefinedAccountType)
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = ManageKeysView()
            val router = ManageKeysRouter()
            val interactor = ManageKeysInteractor(App.accountManager, App.predefinedAccountTypeManager)
            val presenter = ManageKeysPresenter(view, router, interactor)

            interactor.delegate = presenter

            return presenter as T
        }
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, ManageKeysActivity::class.java))
    }
}

data class ManageAccountItem(val predefinedAccountType: PredefinedAccountType, val account: Account?)
