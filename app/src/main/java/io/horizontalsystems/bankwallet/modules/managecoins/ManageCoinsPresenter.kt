package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin

class ManageCoinsPresenter(
        private val interactor: ManageCoinsModule.IInteractor,
        private val router: ManageCoinsModule.IRouter,
        private val state: ManageCoinsModule.ManageCoinsPresenterState
) : ManageCoinsModule.IViewDelegate, ManageCoinsModule.IInteractorDelegate {


    var view: ManageCoinsModule.IView? = null

    override fun viewDidLoad() {
        view?.setTitle(R.string.ManageCoins_title)
        interactor.loadCoins()
    }

    override fun didLoadCoins(allCoins: List<Coin>, enabledCoins: List<Coin>) {
        state.allCoins = allCoins.toMutableList()
        state.enabledCoins = enabledCoins.toMutableList()
        view?.updateCoins()
    }

    override fun enableCoin(coin: Coin) {
        state.enable(coin)
        view?.updateCoins()
    }

    override fun disableCoin(coin: Coin) {
        state.disable(coin)
        view?.updateCoins()
    }

    override fun moveCoin(coin: Coin, index: Int) {
        state.move(coin, index)
        view?.updateCoins()
    }

    override fun saveChanges() {
        interactor.saveEnabledCoins(state.enabledCoins)
    }

    override fun didSaveChanges() {
        router.close()
    }

    override fun didFailedToSave() {
        view?.showFailedToSaveError()
    }

    override fun enabledItemForIndex(position: Int): Coin {
        return state.enabledCoins[position]
    }

    override fun disabledItemForIndex(position: Int): Coin {
        return state.disabledCoins[position]
    }

    override val enabledCoinsCount: Int
        get() = state.enabledCoins.size

    override val disabledCoinsCount: Int
        get() = state.disabledCoins.size
}
