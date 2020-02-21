package io.horizontalsystems.bankwallet.modules.managecoins

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem
import io.horizontalsystems.bankwallet.modules.managecoins.views.ManageWalletsActivity

object ManageWalletsModule {

    interface IView {
        fun showNoAccountDialog(coin: Coin, predefinedAccountType: PredefinedAccountType)
        fun showSuccess()
        fun showError(e: Exception)
        fun setItems(coinViewItems: List<CoinManageViewItem>)
    }

    interface IViewDelegate {
        fun onLoad()

        fun onEnable(coin: Coin)
        fun onDisable(coin: Coin)
        fun onSelect(coin: Coin)
        fun onSelectNewAccount(predefinedAccountType: PredefinedAccountType)
        fun onSelectRestoreAccount(predefinedAccountType: PredefinedAccountType)

        fun didRestore(accountType: AccountType)
        fun onClickCancel()
        fun onCoinSettingsClose()
    }

    interface IInteractor {
        val coins: List<Coin>
        val featuredCoins: List<Coin>
        val accounts: List<Account>
        val wallets: List<Wallet>

        fun loadAccounts()
        fun loadWallets()
        fun save(wallet: Wallet)
        fun delete(wallet: Wallet)

        fun createAccount(predefinedAccountType: PredefinedAccountType): Account
        fun createRestoredAccount(accountType: AccountType): Account
        fun save(account: Account)

        fun getCoinSettings(coinType: CoinType): CoinSettings
    }

    interface IRouter {
        fun showCoinSettings()
        fun openRestore(predefinedAccountType: PredefinedAccountType)
        fun close()
    }

    fun start(context: Context, showCloseButton: Boolean) {
        val intent = Intent(context, ManageWalletsActivity::class.java)
        intent.putExtra(ModuleField.SHOW_CLOSE_BUTTON, showCloseButton)
        context.startActivity(intent)
    }

    class Factory(private val showCloseButton: Boolean, private val isColdStart: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = ManageWalletsView()
            val router = ManageWalletsRouter()
            val interactor = ManageWalletsInteractor(App.appConfigProvider, App.walletManager, App.accountManager, App.accountCreator, App.coinSettingsManager)
            val presenter = ManageWalletsPresenter(interactor, isColdStart, showCloseButton, router, view)

            return presenter as T
        }
    }
}
