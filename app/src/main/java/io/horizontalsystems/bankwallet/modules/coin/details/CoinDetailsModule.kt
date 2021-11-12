package io.horizontalsystems.bankwallet.modules.coin.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.coin.CoinViewFactory
import io.horizontalsystems.marketkit.models.FullCoin

object CoinDetailsModule {

    class Factory(private val fullCoin: FullCoin) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = CoinDetailsService(fullCoin, App.marketKit, App.currencyManager)

            return CoinDetailsViewModel(service, CoinViewFactory(App.currencyManager.baseCurrency, App.numberFormatter), App.numberFormatter) as T
        }
    }
}