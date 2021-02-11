package io.horizontalsystems.bankwallet.modules.market.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.list.MarketListService
import io.horizontalsystems.bankwallet.modules.market.list.MarketListViewModel

object MarketFavoritesModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketFavoritesService(App.xRateManager, App.marketFavoritesManager)
            val listService = MarketListService(MarketModule.currencyUSD, service)
            return MarketListViewModel(listService, App.connectivityManager, listOf(listService)) as T
        }

    }

}

