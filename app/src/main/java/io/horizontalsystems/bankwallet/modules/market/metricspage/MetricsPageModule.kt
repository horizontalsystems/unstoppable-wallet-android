package io.horizontalsystems.bankwallet.modules.market.metricspage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.marketglobal.MarketGlobalFetcher
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFactory
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartService
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType

object MetricsPageModule {

    class Factory(private val metricsType: MetricsType) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) = when (modelClass) {
            MetricsPageViewModel::class.java -> {
                val fetcher = MarketGlobalFetcher(App.xRateManager, metricsType)
                val service = MetricChartService(App.currencyManager.baseCurrency, fetcher)

                val factory = MetricChartFactory(App.numberFormatter)

                MetricsPageViewModel(fetcher, service, factory, listOf(service)) as T
            }
            MetricsPageListViewModel::class.java -> {
                val service by lazy {
                    MetricsPageListService(App.xRateManager, App.currencyManager)
                }
                MetricsPageListViewModel(service, App.connectivityManager, listOf(service)) as T
            }
            else -> throw IllegalArgumentException()
        }
    }
}
