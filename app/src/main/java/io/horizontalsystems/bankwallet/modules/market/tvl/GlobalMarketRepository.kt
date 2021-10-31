package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
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
        chartType: ChartView.ChartType
    ): Single<List<MetricChartModule.Item>> {
        return marketKit.globalMarketPointsSingle(currencyCode, getTimePeriod(chartType))
            .map { list ->
                list.map { MetricChartModule.Item(it.tvl, it.timestamp) }
            }
    }

    fun getTvlData(
        currency: Currency,
        chain: TvlModule.Chain,
        chartType: ChartView.ChartType,
        sortDescending: Boolean
    ): Single<List<TvlModule.CoinTvlItem>> {
        // TODO stub endpoint, need to replace after actual endpoint is ready
        return marketKit.marketInfosSingle(50)
            .map { marketInfoList ->
                val coinTvlItems = marketInfoList.mapIndexed { index, marketInfo ->
                    MarketItem.createFromCoinMarket(
                        marketInfo,
                        currency,
                    ).let {
                        val diffPercent = it.diff ?: BigDecimal.ZERO
                        TvlModule.CoinTvlItem(
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
