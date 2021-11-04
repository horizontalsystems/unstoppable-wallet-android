package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.TimePeriod
import io.reactivex.Single
import java.math.BigDecimal

class GlobalMarketRepository(
    private val marketKit: MarketKit
) {

    fun getGlobalMarketPoints(
        currencyCode: String,
        chartType: ChartView.ChartType,
        metricsType: MetricsType
    ): Single<List<MetricChartModule.Item>> {
        return marketKit.globalMarketPointsSingle(currencyCode, getTimePeriod(chartType))
            .map { list ->
                list.map { point ->
                    val value = when (metricsType) {
                        MetricsType.TotalMarketCap -> point.marketCap
                        MetricsType.BtcDominance -> point.dominanceBtc
                        MetricsType.Volume24h -> point.volume24h
                        MetricsType.DefiCap -> point.marketCapDefi
                        MetricsType.TvlInDefi -> point.tvl
                    }
                    MetricChartModule.Item(value, point.timestamp)
                }
            }
    }

    fun getMarketItems(
        currency: Currency,
        sortDescending: Boolean,
        metricsType: MetricsType
    ): Single<List<MarketItem>> {
        return marketKit.marketInfosSingle(250)
            .map { coinMarkets ->
                val marketItems = coinMarkets.map { MarketItem.createFromCoinMarket(it, currency) }
                val sortingField = when (metricsType) {
                    MetricsType.Volume24h -> if (sortDescending) SortingField.HighestVolume else SortingField.LowestVolume
                    else -> if (sortDescending) SortingField.HighestCap else SortingField.LowestCap
                }
                marketItems.sort(sortingField)
            }
    }

    fun getMarketTvlItems(
        currency: Currency,
        chain: TvlModule.Chain,
        chartType: ChartView.ChartType,
        sortDescending: Boolean
    ): Single<List<TvlModule.MarketTvlItem>> {
        // TODO stub endpoint, need to replace after actual endpoint is ready
        return marketKit.marketInfosSingle(250)
            .map { marketInfoList ->
                val coinTvlItems = marketInfoList.mapIndexed { index, marketInfo ->
                    MarketItem.createFromCoinMarket(
                        marketInfo,
                        currency,
                    ).let {
                        val diffPercent = it.diff ?: BigDecimal.ZERO
                        TvlModule.MarketTvlItem(
                            it.fullCoin,
                            it.marketCap,
                            it.marketCap.copy(value = it.marketCap.value * diffPercent.divide(BigDecimal(100))),
                            diffPercent,
                            (index + 1).toString()
                        )
                    }

                }
                if (sortDescending) {
                    coinTvlItems.sortedByDescending { it.tvl.value }
                } else {
                    coinTvlItems.sortedBy { it.tvl.value }
                }
            }
    }

    private fun getTimePeriod(chartType: ChartView.ChartType): TimePeriod {
        return when (chartType) {
            ChartView.ChartType.DAILY -> TimePeriod.Hour24
            ChartView.ChartType.WEEKLY -> TimePeriod.Day7
            ChartView.ChartType.MONTHLY -> TimePeriod.Day30
            else -> throw IllegalArgumentException("Wrong ChartType")
        }
    }

}
