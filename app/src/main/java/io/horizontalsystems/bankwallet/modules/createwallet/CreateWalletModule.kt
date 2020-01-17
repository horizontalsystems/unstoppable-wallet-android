package io.horizontalsystems.bankwallet.modules.createwallet

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.EosUnsupportedException
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem
import io.horizontalsystems.bankwallet.modules.createwallet.view.CreateWalletActivity

object CreateWalletModule {

    interface IView {
        fun setItems(allCoinViewItems: List<CoinManageViewItem>)
        fun setCreateButton(enabled: Boolean)
        fun showNotSupported(predefinedAccountType: PredefinedAccountType)
    }

    interface IRouter {
        fun startMainModule()
        fun close()
    }

    interface IViewDelegate {
        fun onLoad()
        fun onEnable(coin: Coin)
        fun onDisable(coin: Coin)
        fun onSelect(coin: Coin)
        fun onCreateButtonClick()
    }

    interface IInteractor {
        val coins: List<Coin>
        val featuredCoins: List<Coin>

        @Throws(EosUnsupportedException::class)
        fun createAccounts(accounts: List<Account>)
        @Throws
        fun account(predefinedAccountType: PredefinedAccountType) : Account
        fun coinSettings(coinType: CoinType): CoinSettings
        fun saveWallets(wallets: List<Wallet>)
    }

    class Factory(private val presentationMode: PresentationMode, private val predefinedAccountType: PredefinedAccountType?) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = CreateWalletView()
            val router = CreateWalletRouter()
            val interactor = CreateWalletInteractor(App.appConfigProvider, App.accountCreator, App.accountManager, App.walletManager, App.coinSettingsManager)
            val presenter = CreateWalletPresenter(presentationMode, predefinedAccountType, view, router, interactor)

            return presenter as T
        }
    }

    fun startInApp(context: Context, predefinedAccountType: PredefinedAccountType) {
        val intent = Intent(context, CreateWalletActivity::class.java)
        intent.putParcelableExtra(ModuleField.PRESENTATION_MODE, PresentationMode.InApp)
        intent.putParcelableExtra(ModuleField.PREDEFINED_ACCOUNT_TYPE, predefinedAccountType)
        context.startActivity(intent)
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, CreateWalletActivity::class.java))
    }

}
