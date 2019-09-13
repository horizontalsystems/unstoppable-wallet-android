package io.horizontalsystems.bankwallet.modules.createwallet

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.FeaturedCoin

object CreateWalletModule {

    interface IView {
        fun setItems(items: List<CoinViewItem>)
        fun setCreateEnabled(enabled: Boolean)
    }

    interface IRouter {
        fun startMainModule()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun didEnable(position: Int)
        fun didDisable(position: Int)
        fun didCreate()
    }

    interface IInteractor {
        val featuredCoins: List<FeaturedCoin>

        fun createWallet(coins: List<Coin>)
    }

    class State {
        var coins = listOf<Coin>()
        var enabledPositions = setOf<Int>()
    }

    data class CoinViewItem(val title: String, val code: String, val selected: Boolean)

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = CreateWalletView()
            val router = CreateWalletRouter()
            val interactor = CreateWalletInteractor(App.appConfigProvider)
            val presenter = CreateWalletPresenter(view, router, interactor, State())

            return presenter as T
        }
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, CreateWalletActivity::class.java))
    }
}
