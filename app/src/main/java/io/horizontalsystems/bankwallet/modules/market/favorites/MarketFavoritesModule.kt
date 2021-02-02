package io.horizontalsystems.bankwallet.modules.market.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.MarketModule

object MarketFavoritesModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketFavoritesService(MarketModule.currencyUSD, App.xRateManager, App.marketFavoritesManager)
            return MarketFavoritesViewModel(service, App.connectivityManager, listOf(service)) as T
        }

    }

}

