package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.Indicator
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.DefiMarketInfo
import io.horizontalsystems.marketkit.models.TimePeriod
import io.reactivex.Single
import java.math.BigDecimal

class GlobalMarketRepository(
    private val marketKit: MarketKit
) {

    private var cache: List<DefiMarketInfo> = listOf()

    fun getGlobalMarketPoints(
        currencyCode: String,
        chartType: ChartView.ChartType,
        metricsType: MetricsType
    ): Single<List<MetricChartModule.Item>> {
        return marketKit.globalMarketPointsSingle(currencyCode, getTimePeriod(chartType))
            .map { list ->
                list.map { point ->
                    val additional = mutableMapOf<Indicator, BigDecimal>()
                    val value: BigDecimal
                    when (metricsType) {
                        MetricsType.TotalMarketCap -> {
                            additional[Indicator.Dominance] = point.dominanceBtc
                            value = point.marketCap
                        }
                        MetricsType.BtcDominance -> value = point.dominanceBtc
                        MetricsType.Volume24h -> value = point.volume24h
                        MetricsType.DefiCap -> value = point.marketCapDefi
                        MetricsType.TvlInDefi -> value = point.tvl
                    }
                    MetricChartModule.Item(value, additional, point.timestamp)
                }
            }
    }

    fun getMarketItems(
        currency: Currency,
        sortDescending: Boolean,
        metricsType: MetricsType
    ): Single<List<MarketItem>> {
        return marketKit.marketInfosSingle(250, currency.code, defi = metricsType == MetricsType.DefiCap)
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
        sortDescending: Boolean,
        forceRefresh: Boolean
    ): Single<List<TvlModule.MarketTvlItem>> =
        Single.create { emitter ->
            try {
                val defiMarketInfos = defiMarketInfos(currency.code, forceRefresh)
                val marketTvlItems = getMarketTvlItems(defiMarketInfos, currency, chain, chartType, sortDescending)
                emitter.onSuccess(marketTvlItems)
            } catch (error: Throwable) {
                emitter.onError(error)
            }
        }

    private fun defiMarketInfos(currencyCode: String, forceRefresh: Boolean): List<DefiMarketInfo> =
        if (forceRefresh || cache.isEmpty()) {
            val defiMarketInfo = marketKit.defiMarketInfosSingle(currencyCode).blockingGet()

            cache = defiMarketInfo

            defiMarketInfo
        } else {
            cache
        }

    private fun getMarketTvlItems(
        defiMarketInfoList: List<DefiMarketInfo>,
        currency: Currency,
        chain: TvlModule.Chain,
        chartType: ChartView.ChartType,
        sortDescending: Boolean
    ): List<TvlModule.MarketTvlItem> {
        val tvlItems = defiMarketInfoList.map { defiMarketInfo ->
            val diffPercent: BigDecimal? = when (chartType) {
                ChartView.ChartType.DAILY -> defiMarketInfo.tvlChange1D
                ChartView.ChartType.WEEKLY -> defiMarketInfo.tvlChange7D
                ChartView.ChartType.MONTHLY -> defiMarketInfo.tvlChange30D
                else -> null
            }
            val diff: CurrencyValue? = diffPercent?.let {
                CurrencyValue(currency, defiMarketInfo.tvl * it.divide(BigDecimal(100)))
            }

            TvlModule.MarketTvlItem(
                defiMarketInfo.fullCoin,
                defiMarketInfo.name,
                defiMarketInfo.chains,
                defiMarketInfo.logoUrl,
                CurrencyValue(currency, defiMarketInfo.tvl),
                diff,
                diffPercent,
                defiMarketInfo.tvlRank.toString()
            )
        }

        val chainTvlItems = if (chain == TvlModule.Chain.All) {
            tvlItems
        } else {
            tvlItems.filter { it.chains.contains(chain.name) }
        }

        return if (sortDescending) {
            chainTvlItems.sortedByDescending { it.tvl.value }
        } else {
            chainTvlItems.sortedBy { it.tvl.value }
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
