package io.horizontalsystems.bankwallet.modules.market.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object MarketMetricsModule {
    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketMetricsService(App.xRateManager, App.currencyManager)
            return MarketMetricsViewModel(service, listOf(service)) as T
        }

    }

}
