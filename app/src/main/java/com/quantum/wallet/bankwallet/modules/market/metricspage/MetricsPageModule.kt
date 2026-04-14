package com.quantum.wallet.bankwallet.modules.market.metricspage

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.chart.ChartCurrencyValueFormatterShortened
import com.quantum.wallet.bankwallet.modules.chart.ChartModule
import com.quantum.wallet.bankwallet.modules.chart.ChartViewModel
import com.quantum.wallet.bankwallet.modules.market.MarketDataValue
import com.quantum.wallet.bankwallet.modules.market.MarketModule
import com.quantum.wallet.bankwallet.modules.market.tvl.GlobalMarketRepository
import com.quantum.wallet.bankwallet.modules.metricchart.MetricsType
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

