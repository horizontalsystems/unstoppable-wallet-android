package io.horizontalsystems.bankwallet.modules.managecoins

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
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
        fun viewDidLoad()

        fun onEnable(coin: Coin)
        fun onDisable(coin: Coin)
        fun onSelect(coin: Coin)
        fun onSelectNewAccount(predefinedAccountType: PredefinedAccountType)
        fun onSelectRestoreAccount(predefinedAccountType: PredefinedAccountType)

        fun didRestore(accountType: AccountType)
        fun onClickCancel()
        fun onSelect(coinSettings: MutableMap<CoinSetting, String>, coin: Coin)
    }

    interface IInteractor {
        val coins: List<Coin>
        val featuredCoins: List<Coin>
        val accounts: List<Account>
        val wallets: List<Wallet>

        fun save(wallet: Wallet)
        fun delete(wallet: Wallet)

        fun createAccount(predefinedAccountType: PredefinedAccountType): Account
        fun createRestoredAccount(accountType: AccountType): Account
        fun save(account: Account)

        fun coinSettingsToSave(coin: Coin, origin: AccountOrigin, requestedCoinSettings: MutableMap<CoinSetting, String>): CoinSettings
        fun coinSettingsToRequest(coin: Coin, origin: AccountOrigin): CoinSettings
    }

    interface IInteractorDelegate {
    }

    interface IRouter {
        fun showCoinSettings(coin: Coin, coinSettingsToRequest: CoinSettings)
        fun openRestore(predefinedAccountType: PredefinedAccountType)
        fun close()
    }

    fun init(view: ManageWalletsViewModel, router: IRouter) {
        val interactor = ManageWalletsInteractor(App.appConfigProvider, App.walletManager, App.accountManager, App.accountCreator, App.coinSettingsManager)
        val presenter = ManageWalletsPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(context: Context) {
        val intent = Intent(context, ManageWalletsActivity::class.java)
        context.startActivity(intent)
    }
}
