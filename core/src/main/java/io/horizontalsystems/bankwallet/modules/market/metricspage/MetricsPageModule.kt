package io.horizontalsystems.bankwallet.modules.market.metricspage

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.chart.ChartCurrencyValueFormatterShortened
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.tvl.GlobalMarketRepository
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal

object MetricsPageModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(private val metricsType: MetricsType) : ViewModelProvider.Factory {
        private val globalMarketRepository by lazy {
            GlobalMarketRepository(App.marketKit)
        }

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                MetricsPageViewModel::class.java -> {
                    MetricsPageViewModel(metricsType, App.currencyManager, App.marketKit) as T
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
    data class CoinViewItem(
        val fullCoin: FullCoin,
        val subtitle: String,
        val coinRate: String,
        val marketDataValue: MarketDataValue?,
        val rank: String?,
        val sortField: BigDecimal?,
    )

    @Immutable
    data class UiState(
        val header: MarketModule.Header,
        val viewItems: List<CoinViewItem>,
        val viewState: ViewState,
        val isRefreshing: Boolean,
        val toggleButtonTitle: String,
        val sortDescending: Boolean,
    )
}

