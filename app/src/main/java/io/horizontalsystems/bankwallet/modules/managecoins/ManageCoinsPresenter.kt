package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.entities.Coin

class ManageCoinsPresenter(
        private val interactor: ManageCoinsModule.IInteractor,
        private val router: ManageCoinsModule.IRouter,
        private val state: ManageCoinsModule.ManageCoinsPresenterState
) : ManageCoinsModule.IViewDelegate, ManageCoinsModule.IInteractorDelegate {


    var view: ManageCoinsModule.IView? = null

    override fun viewDidLoad() {
        interactor.loadCoins()
    }

    override fun didLoadCoins(allCoins: List<Coin>, enabledCoins: List<Coin>) {
        state.allCoins = allCoins.toMutableList()
        state.enabledCoins = enabledCoins.toMutableList()
        updateCoins()
    }

    private fun updateCoins() {
        view?.showCoins(state.enabledCoins, state.disabledCoins)
    }

    override fun enableCoin(coin: Coin) {
        state.enable(coin)
        updateCoins()
    }

    override fun disableCoin(coin: Coin) {
        state.disable(coin)
        updateCoins()
    }

    override fun moveCoin(coin: Coin, index: Int) {
        state.move(coin, index)
        updateCoins()
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
}
