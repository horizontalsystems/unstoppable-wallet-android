package cash.p.terminal.modules.market.metricspage

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.chart.ChartCurrencyValueFormatterShortened
import cash.p.terminal.modules.chart.ChartModule
import cash.p.terminal.modules.chart.ChartViewModel
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.MarketViewItem
import cash.p.terminal.modules.market.tvl.GlobalMarketRepository
import cash.p.terminal.modules.metricchart.MetricsType
import cash.p.terminal.ui.compose.Select

object MetricsPageModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(private val metricsType: MetricsType) : ViewModelProvider.Factory {
        private val globalMarketRepository by lazy {
            GlobalMarketRepository(App.marketKit)
        }

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                MetricsPageViewModel::class.java -> {
                    val service = MetricsPageService(metricsType, App.currencyManager, globalMarketRepository)
                    MetricsPageViewModel(service) as T
                }
                ChartViewModel::class.java -> {
                    val chartService = MetricsPageChartService(App.currencyManager, metricsType, globalMarketRepository)
                    val chartNumberFormatter = ChartCurrencyValueFormatterShortened()
                    ChartModule.createViewModel(chartService, chartNumberFormatter) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    @Immutable
    data class MarketData(
        val menu: Menu,
        val marketViewItems: List<MarketViewItem>
    )

    @Immutable
    data class Menu(
        val sortDescending: Boolean,
        val marketFieldSelect: Select<MarketField>
    )
}

