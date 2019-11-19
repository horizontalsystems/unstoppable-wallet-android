package io.horizontalsystems.bankwallet.modules.managecoins

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.managecoins.views.ManageWalletsActivity

object ManageWalletsModule {

    interface IView {
        fun updateCoins()
        fun showNoAccountDialog(coin: Coin, predefinedAccountType: IPredefinedAccountType)
        fun showSuccess()
        fun showError(e: Exception)
    }

    interface IViewDelegate {
        fun viewDidLoad()

        fun onClickCreateKey()
        fun onClickRestoreKey()
        fun onClickCancel()
        fun onRestore(accountType: AccountType, syncMode: SyncMode? = null)

        val popularItemsCount: Int
        fun popularItem(position: Int): ManageWalletViewItem
        val itemsCount: Int
        fun item(position: Int): ManageWalletViewItem

        fun enablePopularCoin(position: Int)
        fun disablePopularCoin(position: Int)

        fun enableCoin(position: Int)
        fun disableCoin(position: Int)

        fun saveChanges()
        fun onClear() {}
    }

    interface IInteractor {
        val coins: List<Coin>
        val wallets: List<Wallet>
        val predefinedAccountTypes: List<IPredefinedAccountType>
        fun wallet(coin: Coin): Wallet?
        fun saveWallets(wallets: List<Wallet>)
        fun createWallet(coin: Coin): Wallet
        fun restoreWallet(coin: Coin, accountType: AccountType, syncMode: SyncMode?): Wallet
    }

    interface IInteractorDelegate {
        fun didSaveChanges()
    }

    interface IRouter {
        fun openRestoreWordsModule(wordsCount: Int, titleRes: Int)
        fun openRestoreEosModule(titleRes: Int)
        fun close()
    }

    fun init(view: ManageWalletsViewModel, router: IRouter) {
        val interactor = ManageWalletsInteractor(App.appConfigProvider, App.walletManager, App.accountCreator, App.walletFactory, App.predefinedAccountTypeManager)
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
