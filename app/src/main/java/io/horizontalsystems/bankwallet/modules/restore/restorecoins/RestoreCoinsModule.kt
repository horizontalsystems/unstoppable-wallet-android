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
    }

    interface IRouter {
        fun startMainModule()
        fun close()
    }

    interface IViewDelegate {
        fun onLoad()
        fun onEnable(coin: Coin)
        fun onDisable(coin: Coin)
        fun onProceedButtonClick()
    }

    interface IInteractor {
        val coins: List<Coin>
        val featuredCoins: List<Coin>

        @Throws(EosUnsupportedException::class)
        fun createAccounts(accounts: List<Account>)
        @Throws
        fun account(accountType: AccountType) : Account
        fun saveWallets(wallets: List<Wallet>)
        fun create(account: Account)
        fun getCoinSettings(coinType: CoinType): CoinSettings
    }

    class Factory(private val presentationMode: PresentationMode, private val predefinedAccountType: PredefinedAccountType, private val accountType: AccountType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = RestoreCoinsView()
            val router = RestoreCoinsRouter()
            val interactor = RestoreCoinsInteractor(App.appConfigProvider, App.accountCreator, App.accountManager, App.walletManager, App.coinSettingsManager)
            val presenter = RestoreCoinsPresenter(presentationMode, predefinedAccountType, accountType, view, router, interactor)

            return presenter as T
        }
    }

    fun start(context: AppCompatActivity, predefinedAccountType: PredefinedAccountType, accountType: AccountType, mode: PresentationMode) {
        val intent = Intent(context, RestoreCoinsActivity::class.java)
        intent.putParcelableExtra(ModuleField.PRESENTATION_MODE, mode)
        intent.putParcelableExtra(ModuleField.PREDEFINED_ACCOUNT_TYPE, predefinedAccountType)
        intent.putParcelableExtra(ModuleField.ACCOUNT_TYPE, accountType)
        context.startActivityForResult(intent, ModuleCode.RESTORE_COINS)
    }

}
