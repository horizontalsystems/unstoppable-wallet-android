package io.horizontalsystems.bankwallet.modules.managecoins

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Wallet
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.SyncMode

object ManageWalletsModule {

    class ManageWalletsPresenterState {
        var allCoins = listOf<Coin>()
            set(value) {
                field = value
                setDisabledCoins()
            }

        var enabledCoins = mutableListOf<Wallet>()
            set(value) {
                field = value
                setDisabledCoins()
            }

        var disabledCoins = listOf<Coin>()

        var restoringKeyForCoin: Coin? = null

        fun enable(coin: Wallet) {
            enabledCoins.add(coin)
            setDisabledCoins()
        }

        fun disable(wallet: Wallet) {
            enabledCoins.remove(wallet)
            setDisabledCoins()
        }

        fun move(wallet: Wallet, index: Int) {
            enabledCoins.remove(wallet)
            enabledCoins.add(index, wallet)
        }

        private fun setDisabledCoins() {
            disabledCoins = allCoins.filter { coin ->
                enabledCoins.find { it.coin === coin } === null
            }
        }
    }

    interface IView {
        fun updateCoins()
        fun showFailedToSaveError()
        fun showRestoreKeyDialog(coin: Coin)
        fun showCreateAndRestoreKeyDialog(coin: Coin)
        fun showFailedToCreateKey()
        fun showFailedToRestoreKey()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onClickCreateKey(coin: Coin)
        fun onClickRestoreKey(coin: Coin)
        fun onRestore(accountType: AccountType, syncMode: SyncMode)
        fun enableCoin(position: Int)
        fun disableCoin(position: Int)
        fun saveChanges()
        fun moveCoin(fromIndex: Int, toIndex: Int)
        fun onClear()

        fun enabledItemForIndex(position: Int): Wallet
        fun disabledItemForIndex(position: Int): Coin
        val enabledCoinsCount: Int
        val disabledCoinsCount: Int
    }

    interface IInteractor {
        fun load()
        fun saveWallets(wallets: List<Wallet>)
        fun clear()
        fun createWalletForCoin(coin: Coin): Wallet
        fun restoreWallet(coin:Coin, accountType: AccountType, syncMode: SyncMode): Wallet
    }

    interface IInteractorDelegate {
        fun didLoad(coins: List<Coin>, wallets: List<Wallet>)
        fun didSaveChanges()
        fun didFailedToSave()
    }

    interface IRouter {
        fun startManageKeysModule()
        fun close()
        fun openRestoreWordsModule()
    }

    fun init(view: ManageWalletsViewModel, router: IRouter) {
        val interactor = ManageWalletsInteractor(App.appConfigProvider, App.accountCreator, App.walletCreator, App.walletManager, App.predefinedAccountTypeManager)
        val presenter = ManageWalletsPresenter(interactor, router, App.walletCreator, ManageWalletsPresenterState())

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(context: Context) {
        val intent = Intent(context, ManageWalletsActivity::class.java)
        context.startActivity(intent)
    }
}
