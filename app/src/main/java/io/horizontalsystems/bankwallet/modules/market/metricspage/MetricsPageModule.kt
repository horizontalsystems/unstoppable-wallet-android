package io.horizontalsystems.bankwallet.modules.market.metricspage

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.chart.ChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfoHeaderItem
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.tvl.GlobalMarketRepository
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFactory
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.core.entities.Currency

object MetricsPageModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val metricsType: MetricsType
    ) : ViewModelProvider.Factory {
        val globalMarketRepository = GlobalMarketRepository(App.marketKit)

        val chartService by lazy {
            val chartRepo = MetricsPageChartRepo(metricsType, globalMarketRepository)
            ChartService(App.currencyManager, chartRepo)
        }

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                MetricsPageViewModel::class.java -> {
                    val service = MetricsPageService(metricsType, App.currencyManager, globalMarketRepository)
                    // todo: how to not depend on chartService in MetricsPageViewModel?
                    MetricsPageViewModel(service, chartService) as T
                }
                ChartViewModel::class.java -> {
                    val factory = MetricChartFactory(App.numberFormatter)
                    ChartViewModel(chartService, factory) as T
                }

                else -> throw IllegalArgumentException()
            }

        }
    }

    @Immutable
    data class ChartData(
        val subtitle: ChartInfoHeaderItem,
        val currency: Currency,
        val chartInfoData: ChartInfoData
    )

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

