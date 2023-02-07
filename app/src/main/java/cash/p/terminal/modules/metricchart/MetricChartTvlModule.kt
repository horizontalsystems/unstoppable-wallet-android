package cash.p.terminal.modules.metricchart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.chart.ChartCurrencyValueFormatterShortened
import cash.p.terminal.modules.chart.ChartModule

object MetricChartTvlModule {

    class Factory(private val coinUid: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val chartService = CoinTvlChartService(App.currencyManager, App.marketKit, coinUid)
            val chartNumberFormatter = ChartCurrencyValueFormatterShortened()
            return ChartModule.createViewModel(chartService, chartNumberFormatter) as T
        }
    }
}