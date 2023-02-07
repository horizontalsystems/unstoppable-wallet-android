package cash.p.terminal.modules.coin.majorholders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.coin.CoinViewFactory

object CoinMajorHoldersModule {
    class Factory(private val coinUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = CoinMajorHoldersService(coinUid, App.marketKit)
            val factory = CoinViewFactory(App.currencyManager.baseCurrency, App.numberFormatter)
            return CoinMajorHoldersViewModel(service, factory) as T
        }
    }
}
