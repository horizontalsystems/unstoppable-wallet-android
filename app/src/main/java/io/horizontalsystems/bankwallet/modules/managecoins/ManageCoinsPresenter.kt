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

    override fun didLoadEnabledCoins(enabledCoins: List<Coin>) {
        state.enabledCoins = enabledCoins.toMutableList()
        view?.updateCoins()
    }

    override fun didLoadAllCoins(allCoins: List<Coin>) {
        state.allCoins = allCoins.toMutableList()
        view?.updateCoins()
    }

    override fun enableCoin(position: Int) {
        state.enable(state.disabledCoins[position])
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
