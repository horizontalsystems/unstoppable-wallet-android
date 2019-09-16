package io.horizontalsystems.bankwallet.modules.createwallet

import androidx.lifecycle.ViewModel

class CreateWalletPresenter(
        val view: CreateWalletModule.IView,
        val router: CreateWalletModule.IRouter,
        private val interactor: CreateWalletModule.IInteractor,
        private val state: CreateWalletModule.State
) : ViewModel(), CreateWalletModule.IViewDelegate {

    override fun viewDidLoad() {
        val coinViewItems = interactor.featuredCoins.map {
            CreateWalletModule.CoinViewItem(it.title, it.code)
        }

        view.setItems(coinViewItems)

        state.coins = interactor.featuredCoins
    }

    override fun didTapItem(position: Int) {
        interactor.createWallet(state.coins[position])
        router.startMainModule()
    }
}
