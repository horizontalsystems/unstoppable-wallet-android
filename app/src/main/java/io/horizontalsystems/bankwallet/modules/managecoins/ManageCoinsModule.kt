package io.horizontalsystems.bankwallet.modules.managecoins

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.entities.Coin

object ManageCoinsModule {

    class ManageCoinsPresenterState {
        var allCoins = listOf<Coin>()
        var enabledCoins = mutableListOf<Coin>()
        val disabledCoins: List<Coin>
            get() {
                return allCoins.minus(enabledCoins)
            }

        fun enable(coin: Coin) {
            enabledCoins.add(coin)
        }

        fun disable(coin: Coin) {
            enabledCoins.remove(coin)
        }

        fun move(coin: Coin, index: Int) {
            enabledCoins.remove(coin)
            enabledCoins.add(index, coin)
        }
    }

    interface IView {
        fun setTitle(title: Int)
        fun updateCoins()
        fun showFailedToSaveError()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun enableCoin(coin: Coin)
        fun disableCoin(coin: Coin)
        fun saveChanges()
        fun moveCoin(coin: Coin, index: Int)

        fun enabledItemForIndex(position: Int): Coin
        fun disabledItemForIndex(position: Int): Coin
        val enabledCoinsCount: Int
        val disabledCoinsCount: Int
    }

    interface IInteractor {
        fun loadCoins()
        fun saveEnabledCoins(enabledCoins: List<Coin>)
    }

    interface IInteractorDelegate {
        fun didLoadCoins(allCoins: List<Coin>, enabledCoins: List<Coin>)
        fun didSaveChanges()
        fun didFailedToSave()
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

    fun start(context: Context) {
        val intent = Intent(context, ManageCoinsActivity::class.java)
        context.startActivity(intent)
    }

}
