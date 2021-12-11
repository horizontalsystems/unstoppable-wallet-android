package io.horizontalsystems.bankwallet.modules.market.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import java.math.BigDecimal

object MarketOverviewModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val topMarketsRepository = TopMarketsRepository(App.marketKit)
            val marketMetricsRepository = MarketMetricsRepository(App.marketKit)
            val service = MarketOverviewService(
                topMarketsRepository,
                marketMetricsRepository,
                App.backgroundManager,
                App.currencyManager
            )
            return MarketOverviewViewModel(service) as T
        }
    }

    sealed class ViewItemState {
        class Error(val error: String) : ViewItemState()
        class Loaded(val viewItem: ViewItem) : ViewItemState()
    }

    @Immutable
    data class ViewItem(
        val marketMetrics: MarketMetrics,
        val boards: List<Board>
    )

    data class MarketMetrics(
        val totalMarketCap: MetricData,
        val btcDominance: MetricData,
        val volume24h: MetricData,
        val defiCap: MetricData,
        val defiTvl: MetricData,
    )

    data class MarketMetricsPoint(
        val value: BigDecimal,
        val timestamp: Long
    )

    data class MarketMetricsItem(
        val currencyCode: String,
        val volume24h: CurrencyValue,
        val volume24hDiff24h: BigDecimal,
        val marketCap: CurrencyValue,
        val marketCapDiff24h: BigDecimal,
        var btcDominance: BigDecimal = BigDecimal.ZERO,
        var btcDominanceDiff24h: BigDecimal = BigDecimal.ZERO,
        var defiMarketCap: CurrencyValue,
        var defiMarketCapDiff24h: BigDecimal = BigDecimal.ZERO,
        var defiTvl: CurrencyValue,
        var defiTvlDiff24h: BigDecimal = BigDecimal.ZERO,
        val totalMarketCapPoints: List<MarketMetricsPoint>,
        val btcDominancePoints: List<MarketMetricsPoint>,
        val volume24Points: List<MarketMetricsPoint>,
        val defiMarketCapPoints: List<MarketMetricsPoint>,
        val defiTvlPoints: List<MarketMetricsPoint>
    )

    data class Board(
        val boardHeader: BoardHeader,
        val marketViewItems: List<MarketViewItem>,
        val type: MarketModule.ListType
    )

    data class BoardHeader(
        val title: Int,
        val iconRes: Int,
        val topMarketSelect: Select<TopMarket>
    )

}
