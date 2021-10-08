package io.horizontalsystems.bankwallet.modules.market.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.GlobalCoinMarket
import java.math.BigDecimal

object MarketOverviewModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service =
                MarketOverviewService(App.marketKit, App.xRateManager, App.backgroundManager, App.currencyManager)
            return MarketOverviewViewModel(service, listOf(service)) as T
        }

    }

    @Immutable
    data class ViewItem(
        val marketMetrics: MarketMetrics,
        val boardItems: List<BoardItem>
    )

    data class MarketMetrics(
        val totalMarketCap: MetricData,
        val btcDominance: MetricData,
        val volume24h: MetricData,
        val defiCap: MetricData,
        val defiTvl: MetricData,
    )

    data class MarketMetricsPoint(val value: BigDecimal, val timestamp: Long)

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
    ) {
        companion object {
            fun createFromGlobalCoinMarket(globalCoinMarket: GlobalCoinMarket, currency: Currency): MarketMetricsItem {
                return MarketMetricsItem(
                    globalCoinMarket.currencyCode,
                    CurrencyValue(currency, globalCoinMarket.volume24h),
                    globalCoinMarket.volume24hDiff24h,
                    CurrencyValue(currency, globalCoinMarket.marketCap),
                    globalCoinMarket.marketCapDiff24h,
                    globalCoinMarket.btcDominance,
                    globalCoinMarket.btcDominanceDiff24h,
                    CurrencyValue(currency, globalCoinMarket.defiMarketCap),
                    globalCoinMarket.defiMarketCapDiff24h,
                    CurrencyValue(currency, globalCoinMarket.defiTvl),
                    globalCoinMarket.defiTvlDiff24h,
                    totalMarketCapPoints = globalCoinMarket.globalCoinMarketPoints.map {
                        MarketMetricsPoint(
                            it.marketCap,
                            it.timestamp
                        )
                    },
                    btcDominancePoints = globalCoinMarket.globalCoinMarketPoints.map {
                        MarketMetricsPoint(
                            it.btcDominance,
                            it.timestamp
                        )
                    },
                    volume24Points = globalCoinMarket.globalCoinMarketPoints.map {
                        MarketMetricsPoint(
                            it.volume24h,
                            it.timestamp
                        )
                    },
                    defiMarketCapPoints = globalCoinMarket.globalCoinMarketPoints.map {
                        MarketMetricsPoint(
                            it.defiMarketCap,
                            it.timestamp
                        )
                    },
                    defiTvlPoints = globalCoinMarket.globalCoinMarketPoints.map {
                        MarketMetricsPoint(
                            it.defiTvl,
                            it.timestamp
                        )
                    }
                )
            }
        }
    }

    data class BoardItem(
        val boardHeader: BoardHeader,
        val boardContent: BoardContent,
        val type: MarketModule.ListType
    )

    data class BoardHeader(val title: Int, val iconRes: Int, val toggleButton: MarketListHeaderView.ToggleButton)
    data class BoardContent(val marketViewItems: List<MarketViewItem>, val showAllClick: MarketModule.ListType)

}
