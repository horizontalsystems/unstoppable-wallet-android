package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.DefaultAccountType
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.Wallet
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsPresenter(
        private val interactor: ManageWalletsModule.IInteractor,
        private val router: ManageWalletsModule.IRouter,
        private val walletManager: IWalletManager,
        private val state: ManageWalletsModule.ManageWalletsPresenterState)
    : ManageWalletsModule.IViewDelegate, ManageWalletsModule.IInteractorDelegate {

    var view: ManageWalletsModule.IView? = null

    private val disposables = CompositeDisposable()

    //  IViewDelegate

    override fun viewDidLoad() {
        interactor.load()
    }

    override fun onClickCreateKey(coin: Coin) {
        val wallet = try {
            interactor.createWallet(coin)
        } catch (ex: Exception) {
            view?.showFailedToCreateKey()
            return
        }

        state.enable(wallet)
        view?.updateCoins()
    }

    override fun onClickRestoreKey(coin: Coin) {
        state.restoringKeyForCoin = coin

        when (coin.type.defaultAccountType) {
            is DefaultAccountType.Mnemonic -> {
                router.openRestoreWordsModule()
            }
        }
    }

    override fun onRestore(accountType: AccountType, syncMode: SyncMode) {
        val coin = state.restoringKeyForCoin ?: return

        val wallet = try {
            interactor.restoreWallet(coin, accountType, syncMode)
        } catch (ex: Exception) {
            view?.showFailedToRestoreKey()
            return
        } finally {
            state.restoringKeyForCoin = null
        }

        state.enable(wallet)
        view?.updateCoins()
    }

    override fun enableCoin(position: Int) {
        val coin = state.disabledCoins[position]

        val wallet = walletManager.wallet(coin)
        if (wallet == null) {
            if (coin.type is CoinType.Eos) {
                view?.showRestoreKeyDialog(coin)
            } else {
                view?.showCreateAndRestoreKeyDialog(coin)
            }
        } else {
            state.enable(wallet)
            view?.updateCoins()
        }
    }

    override fun disableCoin(position: Int) {
        state.disable(state.enabledCoins[position])
        view?.updateCoins()
    }

    override fun moveCoin(fromIndex: Int, toIndex: Int) {
        state.move(state.enabledCoins[fromIndex], toIndex)
        view?.updateCoins()
    }

    override fun saveChanges() {
        interactor.saveWallets(state.enabledCoins)
    }

    override fun enabledItemForIndex(position: Int): Wallet {
        return state.enabledCoins[position]
    }

    override fun disabledItemForIndex(position: Int): Coin {
        return state.disabledCoins[position]
    }

    override val enabledCoinsCount: Int
        get() = state.enabledCoins.size

    override val disabledCoinsCount: Int
        get() = state.disabledCoins.size

    override fun onClear() {
        interactor.clear()
        disposables.clear()
    }

    // IInteractorDelegate

    override fun didLoad(coins: List<Coin>, wallets: List<Wallet>) {
        state.allCoins = coins.toMutableList()
        state.enabledCoins = wallets.toMutableList()
        view?.updateCoins()
    }

    override fun didSaveChanges() {
        router.close()
    }

    override fun didFailedToSave() {
        view?.showFailedToSaveError()
    }
}
