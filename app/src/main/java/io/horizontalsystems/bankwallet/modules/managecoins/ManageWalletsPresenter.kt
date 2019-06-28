package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.Wallet
import io.horizontalsystems.bankwallet.entities.Coin

class ManageWalletsPresenter(private val interactor: ManageWalletsModule.IInteractor, private val router: ManageWalletsModule.IRouter, private val state: ManageWalletsModule.ManageWalletsPresenterState)
    : ManageWalletsModule.IViewDelegate, ManageWalletsModule.IInteractorDelegate {

    var view: ManageWalletsModule.IView? = null

    // IViewDelegate

    override fun viewDidLoad() {
        interactor.load()
    }

    override fun enableCoin(position: Int) {
        val coin = state.disabledCoins[position]
        val account = interactor.accounts(coin.type).firstOrNull() ?: return
        val wallet = Wallet(coin, account, account.defaultSyncMode)

        state.enable(wallet)
        view?.updateCoins()
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
    }

    // IInteractorDelegate

    override fun didLoad(coins: List<Coin>, wallets: List<Wallet>) {
        state.allCoins = coins.toMutableList()
        state.enabledCoins = wallets.toMutableList()
    }

    override fun didSaveChanges() {
        router.close()
    }

    override fun didFailedToSave() {
        view?.showFailedToSaveError()
    }
}
