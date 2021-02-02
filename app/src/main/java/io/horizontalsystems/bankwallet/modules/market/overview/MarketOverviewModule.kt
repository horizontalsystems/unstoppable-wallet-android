package io.horizontalsystems.bankwallet.modules.market.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.top.MarketListTopDataSource

object MarketOverviewModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketOverviewService(App.currencyManager, MarketListTopDataSource(App.xRateManager), App.xRateManager)
            return MarketOverviewViewModel(service, listOf(service)) as T
        }

    }

}
