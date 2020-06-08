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
        fun showDerivationSelectorDialog(derivationOptions: List<AccountType.Derivation>, selected: AccountType.Derivation, coin: Coin)
        fun setItems(coinViewItems: List<CoinManageViewItem>)
    }

    interface IViewDelegate {
        fun onLoad()

        fun onEnable(coin: Coin)
        fun onDisable(coin: Coin)
        fun onSelect(coin: Coin)
        fun onSelectNewAccount(predefinedAccountType: PredefinedAccountType)
        fun onSelectRestoreAccount(predefinedAccountType: PredefinedAccountType)

        fun onClickCancel()
        fun onAccountRestored()
        fun onBlockchainSettingsApproved()
        fun onBlockchainSettingsCancel()
        fun onSelectDerivationSetting(coin: Coin, derivation: AccountType.Derivation)
        fun onCancelDerivationSelectorDialog(coin: Coin)
    }

    interface IInteractor {
        val coins: List<Coin>
        val featuredCoins: List<Coin>
        val accounts: List<Account>
        val wallets: List<Wallet>

        fun save(wallet: Wallet)
        fun delete(wallet: Wallet)

        fun createAccount(predefinedAccountType: PredefinedAccountType): Account
        fun save(account: Account)
        fun derivationSetting(coinType: CoinType): DerivationSetting?
        fun saveDerivationSetting(derivationSetting: DerivationSetting)
        fun initializeSettingsWithDefault(coinType: CoinType)
        fun initializeSettings(coinType: CoinType)
    }

    interface IRouter {
        fun openRestore(predefinedAccountType: PredefinedAccountType)
        fun close()
    }

    fun start(context: Context) {
        val intent = Intent(context, ManageWalletsActivity::class.java)
        context.startActivity(intent)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = ManageWalletsView()
            val router = ManageWalletsRouter()
            val interactor = ManageWalletsInteractor(App.coinManager, App.walletManager, App.accountManager, App.accountCreator, App.blockchainSettingsManager)
            val presenter = ManageWalletsPresenter(interactor, router, view)

            return presenter as T
        }
    }
}
