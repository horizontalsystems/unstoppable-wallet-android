package io.horizontalsystems.bankwallet.modules.settings.managekeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

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
        fun onConfirmUnlink(account: Account)
        fun onClickAddressFormat(item: ManageAccountItem)
    }

    interface Interactor {
        val predefinedAccountTypes: List<PredefinedAccountType>
        fun account(predefinedAccountType: PredefinedAccountType): Account?

        fun loadAccounts()
        fun deleteAccount(account: Account)
        fun clear()
    }

    interface InteractorDelegate {
        fun didLoad(accounts: List<ManageAccountItem>)
    }

    interface IRouter {
        fun close()
        fun showCreateWallet(predefinedAccountType: PredefinedAccountType)
        fun showBackup(account: Account, predefinedAccountType: PredefinedAccountType)
        fun showRestore(predefinedAccountType: PredefinedAccountType)
        fun showAddressFormat()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = ManageKeysView()
            val router = ManageKeysRouter()
            val interactor = ManageKeysInteractor(App.accountManager, App.derivationSettingsManager, App.predefinedAccountTypeManager, App.bitcoinCashCoinTypeManager, App.priceAlertManager)
            val presenter = ManageKeysPresenter(view, router, interactor)

            interactor.delegate = presenter

            return presenter as T
        }
    }
}

data class ManageAccountItem(
        val predefinedAccountType: PredefinedAccountType,
        val account: Account?,
        val hasDerivationSetting: Boolean = false
)
