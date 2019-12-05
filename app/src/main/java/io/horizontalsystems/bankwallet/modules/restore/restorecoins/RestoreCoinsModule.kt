package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.EosUnsupportedException
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem

object RestoreCoinsModule {

    interface IView {
        fun setItems(coinViewItems: List<CoinManageViewItem>)
        fun setProceedButton(enabled: Boolean)
        fun setTitle(predefinedAccountType: PredefinedAccountType)
    }

    interface IRouter {
        fun startMainModule()
        fun showCoinSettings(coin: Coin, coinSettingsToRequest: CoinSettings)
        fun showRestore(predefinedAccountType: PredefinedAccountType)
        fun close()
    }

    interface IViewDelegate {
        fun onLoad()
        fun onEnable(coin: Coin)
        fun onDisable(coin: Coin)
        fun onProceedButtonClick()
        fun onSelectCoinSettings(coinSettings: CoinSettings, coin: Coin)
        fun onCancelSelectingCoinSettings()
        fun didRestore(accountType: AccountType)
    }

    interface IInteractor {
        val coins: List<Coin>
        val featuredCoins: List<Coin>

        @Throws(EosUnsupportedException::class)
        fun createAccounts(accounts: List<Account>)
        @Throws
        fun account(accountType: AccountType) : Account
        fun coinSettingsToRequest(coin: Coin, accountOrigin: AccountOrigin): CoinSettings
        fun coinSettingsToSave(coin: Coin, accountOrigin: AccountOrigin, requestedCoinSettings: CoinSettings): CoinSettings
        fun saveWallets(wallets: List<Wallet>)
        fun create(account: Account)
    }

    class Factory(private val presentationMode: PresentationMode, private val predefinedAccountType: PredefinedAccountType) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = RestoreCoinsView()
            val router = RestoreCoinsRouter()
            val interactor = RestoreCoinsInteractor(App.appConfigProvider, App.accountCreator, App.accountManager, App.walletManager, App.coinSettingsManager)
            val presenter = RestoreCoinsPresenter(presentationMode, predefinedAccountType, view, router, interactor)

            return presenter as T
        }
    }

    fun start(context: AppCompatActivity, predefinedAccountType: PredefinedAccountType, mode: PresentationMode) {
        val intent = Intent(context, RestoreCoinsActivity::class.java)
        intent.putParcelableExtra(ModuleField.PRESENTATION_MODE, mode)
        intent.putExtra(ModuleField.PREDEFINED_ACCOUNT_TYPE, predefinedAccountType.toString())
        context.startActivityForResult(intent, ModuleCode.RESTORE_COINS)
    }

}
