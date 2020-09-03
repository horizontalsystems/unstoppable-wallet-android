package io.horizontalsystems.bankwallet.modules.settings.managekeys

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
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
        fun onConfirmUnlink(account: Account)
        fun onClickAdvancedSettings(item: ManageAccountItem)
    }

    interface Interactor {
        val predefinedAccountTypes: List<PredefinedAccountType>
        fun account(predefinedAccountType: PredefinedAccountType): Account?

        fun getWallets(): List<Wallet>
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
        fun showBlockchainSettings(enabledCoinTypes: List<CoinType>)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = ManageKeysView()
            val router = ManageKeysRouter()
            val interactor = ManageKeysInteractor(App.accountManager, App.walletManager, App.blockchainSettingsManager, App.predefinedAccountTypeManager, App.priceAlertManager)
            val presenter = ManageKeysPresenter(view, router, interactor)

            interactor.delegate = presenter

            return presenter as T
        }
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, ManageKeysActivity::class.java))
    }
}

data class ManageAccountItem(
        val predefinedAccountType: PredefinedAccountType,
        val account: Account?,
        val hasDerivationSetting: Boolean = false )
