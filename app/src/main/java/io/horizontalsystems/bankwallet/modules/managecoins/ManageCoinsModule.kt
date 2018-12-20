package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.entities.Coin

object ManageCoinsModule {

    class ManageCoinsPresenterState {
        var allCoins = mutableListOf<Coin>()
        var enabledCoins = mutableListOf<Coin>()
        val disabledCoins: MutableList<Coin>
            get() {
                val disabledCoins = allCoins.toMutableList()
                disabledCoins.removeAll(enabledCoins)
                return disabledCoins
            }

        fun add(coin: Coin) {
            enabledCoins.add(coin)
        }

        fun remove(coin: Coin) {
            enabledCoins.remove(coin)
        }

        fun move(coin: Coin, index: Int) {
            remove(coin)
            enabledCoins.add(index, coin)
        }
    }

    interface IView {
        fun showCoins(enabledCoins: List<Coin>, disabledCoins: List<Coin>)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun enableCoin(coin: Coin)
        fun disableCoin(coin: Coin)
        fun saveChanges()
        fun moveCoin(coin: Coin, index: Int)
    }

    interface IInteractor {
        fun loadCoins()
        fun saveEnabledCoins(enabledCoins: List<Coin>)
    }

    interface IInteractorDelegate {
        fun didLoadCoins(allCoins: List<Coin>, enabledCoins: List<Coin>)
    }

    interface IRouter {
        fun close()
    }


    fun init(view: ManageCoinsViewModel, router: IRouter) {
        val interactor = ManageCoinsInteractor()
        val presenter = ManageCoinsPresenter(interactor, router, ManageCoinsPresenterState())

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
