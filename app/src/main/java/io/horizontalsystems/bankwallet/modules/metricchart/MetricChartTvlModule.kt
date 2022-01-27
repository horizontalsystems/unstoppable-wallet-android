package io.horizontalsystems.bankwallet.modules.metricchart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
import io.horizontalsystems.bankwallet.modules.chart.ChartNumberFormatterShortened

object MetricChartTvlModule {

    class Factory(private val coinUid: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val chartService = CoinTvlChartService(App.currencyManager, App.marketKit, coinUid)
            val chartNumberFormatter = ChartNumberFormatterShortened()
            return ChartModule.createViewModel(chartService, chartNumberFormatter) as T
        }
    }
}