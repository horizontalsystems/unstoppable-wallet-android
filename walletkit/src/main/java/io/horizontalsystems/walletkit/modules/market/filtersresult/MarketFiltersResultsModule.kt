package io.horizontalsystems.walletkit.modules.market.filtersresult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.managers.SignalsControlManager
import io.horizontalsystems.walletkit.modules.market.filters.IMarketListFetcher

object MarketFiltersResultsModule {
    class Factory(val service: IMarketListFetcher) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = MarketFiltersResultService(
                service,
                App.marketFavoritesManager,
                SignalsControlManager(App.localStorage),
                App.marketKit
            )
            return MarketFiltersResultViewModel(service) as T
        }

    }
}
