package io.horizontalsystems.bankwallet.modules.createwallet

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.EosUnsupportedException

class CreateWalletPresenter(
        val view: CreateWalletModule.IView,
        val router: CreateWalletModule.IRouter,
        private val interactor: CreateWalletModule.IInteractor,
        private val coinViewItemFactory: CoinViewItemFactory,
        private val state: CreateWalletModule.State
) : ViewModel(), CreateWalletModule.IViewDelegate {

    override fun viewDidLoad() {
        val defaultSelected = 0
        val featuredCoins = interactor.featuredCoins

        state.coins = featuredCoins
        state.selectedPosition = defaultSelected

        val coinViewItems = coinViewItemFactory.createItems(featuredCoins, defaultSelected)
        view.setItems(coinViewItems)
    }

    override fun didTapItem(position: Int) {
        state.selectedPosition = position

        val coinViewItems = coinViewItemFactory.createItems(state.coins, position)
        view.setItems(coinViewItems)
    }

    override fun didClickCreate() {
        try {
            interactor.createWallet(state.coins[state.selectedPosition])
            router.startMainModule()
        } catch (e: EosUnsupportedException) {
            view.showError(e)
        }
    }
}
