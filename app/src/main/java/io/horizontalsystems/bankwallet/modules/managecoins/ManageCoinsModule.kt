package io.horizontalsystems.bankwallet.modules.managecoins

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin

object ManageCoinsModule {

    class ManageCoinsPresenterState {
        var allCoins = listOf<Coin>()
            set(value) {
                field = value
                setDisabledCoins()
            }

        var enabledCoins = mutableListOf<Coin>()
            set(value) {
                field = value
                setDisabledCoins()
            }

        var disabledCoins = listOf<Coin>()

        fun enable(coin: Coin) {
            enabledCoins.add(coin)
            setDisabledCoins()
        }

        fun disable(coin: Coin) {
            enabledCoins.remove(coin)
            setDisabledCoins()
        }

        fun move(coin: Coin, index: Int) {
            enabledCoins.remove(coin)
            enabledCoins.add(index, coin)
        }

        private fun setDisabledCoins() {
            val coins = allCoins
            disabledCoins = coins.minus(enabledCoins)
        }
    }

    interface IView {
        fun updateCoins()
        fun showFailedToSaveError()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun enableCoin(position: Int)
        fun disableCoin(position: Int)
        fun saveChanges()
        fun moveCoin(fromIndex: Int, toIndex: Int)

        fun enabledItemForIndex(position: Int): Coin
        fun disabledItemForIndex(position: Int): Coin
        val enabledCoinsCount: Int
        val disabledCoinsCount: Int
    }

    interface IInteractor {
        fun syncCoins()
        fun loadCoins()
        fun saveEnabledCoins(enabledCoins: List<Coin>)
    }

    interface IInteractorDelegate {
        fun didLoadEnabledCoins(enabledCoins: List<Coin>)
        fun didLoadAllCoins(allCoins: List<Coin>)
        fun didSaveChanges()
        fun didFailedToSave()
    }

    interface IRouter {
        fun close()
    }


    fun init(view: ManageCoinsViewModel, router: IRouter) {
        val interactor = ManageCoinsInteractor(App.coinManager, App.coinsStorage, App.tokenSyncer)
        val presenter = ManageCoinsPresenter(interactor, router, ManageCoinsPresenterState())

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(context: Context) {
        val intent = Intent(context, ManageCoinsActivity::class.java)
        context.startActivity(intent)
    }

}
