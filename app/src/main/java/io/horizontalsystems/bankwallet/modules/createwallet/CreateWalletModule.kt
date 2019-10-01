package io.horizontalsystems.bankwallet.modules.createwallet

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.EosUnsupportedException
import io.horizontalsystems.bankwallet.entities.Coin

object CreateWalletModule {

    interface IView {
        fun setItems(items: List<CoinViewItem>)
        fun showError(exception: Exception)
    }

    interface IRouter {
        fun startMainModule()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun didTapItem(position: Int)
        fun didClickCreate()
    }

    interface IInteractor {
        val featuredCoins: List<Coin>

        @Throws(EosUnsupportedException::class)
        fun createWallet(coin: Coin)
    }

    class State {
        var selectedPosition: Int = 0
        var coins = listOf<Coin>()
    }

    data class CoinViewItem(val title: String, val code: String) {
        var selected = false

        constructor(title: String, code: String, selected: Boolean) : this(title, code) {
            this.selected = selected
        }
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = CreateWalletView()
            val router = CreateWalletRouter()
            val interactor = CreateWalletInteractor(App.appConfigProvider, App.accountCreator)
            val presenter = CreateWalletPresenter(view, router, interactor, CoinViewItemFactory(), State())

            return presenter as T
        }
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, CreateWalletActivity::class.java))
    }
}
