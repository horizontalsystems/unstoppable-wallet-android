package io.horizontalsystems.bankwallet.modules.createwallet

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Coin

class CreateWalletPresenter(
        val view: CreateWalletModule.IView,
        val router: CreateWalletModule.IRouter,
        private val interactor: CreateWalletModule.IInteractor,
        private val state: CreateWalletModule.State
) : ViewModel(), CreateWalletModule.IViewDelegate {

    override fun viewDidLoad() {
        val coinViewItems = mutableListOf<CreateWalletModule.CoinViewItem>()
        val coins = mutableListOf<Coin>()
        var createEnabled = false

        interactor.featuredCoins.forEach {
            coinViewItems.add(CreateWalletModule.CoinViewItem(it.coin.title, it.coin.code, it.enabledByDefault))
            coins.add(it.coin)
            createEnabled = createEnabled || it.enabledByDefault
        }

        view.setItems(coinViewItems)
        view.setCreateEnabled(createEnabled)

        state.coins = coins
    }

    override fun didEnable(position: Int) {
        var enabledPositions = state.enabledPositions

        if (enabledPositions.isEmpty()) {
            view.setCreateEnabled(true)
        }
        enabledPositions = enabledPositions + position

        state.enabledPositions = enabledPositions
    }

    override fun didDisable(position: Int) {
        var enabledPositions = state.enabledPositions

        enabledPositions = enabledPositions - position
        if (enabledPositions.isEmpty()) {
            view.setCreateEnabled(false)
        }

        state.enabledPositions = enabledPositions
    }

    override fun didCreate() {
        val enabledCoins = state.coins.filterIndexed { index, _ -> state.enabledPositions.contains(index) }
        interactor.createWallet(enabledCoins)
        router.startMainModule()
    }
}
