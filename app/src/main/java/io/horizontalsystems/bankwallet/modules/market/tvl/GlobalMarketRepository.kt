package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.DefiMarketInfo
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single
import java.math.BigDecimal

class GlobalMarketRepository(
    private val marketKit: MarketKitWrapper,
    private val apiTag: String
) {

    private var cache: List<DefiMarketInfo> = listOf()

    fun getGlobalMarketPoints(
        currencyCode: String,
        chartInterval: HsTimePeriod,
        metricsType: MetricsType
    ): Single<List<ChartPoint>> {
        return marketKit.globalMarketPointsSingle(currencyCode, chartInterval)
            .map { list ->
                list.map { point ->
                    val value = when (metricsType) {
                        MetricsType.TotalMarketCap -> point.marketCap
                        MetricsType.BtcDominance -> point.btcDominance
                        MetricsType.Volume24h -> point.volume24h
                        MetricsType.DefiCap -> point.defiMarketCap
                        MetricsType.TvlInDefi -> point.tvl
                    }

                    val dominance = if (metricsType == MetricsType.TotalMarketCap) point.btcDominance.toFloat() else null
                    ChartPoint(value = value.toFloat(), timestamp = point.timestamp, dominance = dominance)
                }
            }
    }

    fun getTvlGlobalMarketPoints(
        chain: String,
        currencyCode: String,
        chartInterval: HsTimePeriod,
    ): Single<List<ChartPoint>> {
        return marketKit.marketInfoGlobalTvlSingle(chain, currencyCode, chartInterval)
            .map { list ->
                list.map { point ->
                      ChartPoint(point.value.toFloat(), point.timestamp)
                }
            }
    }

    fun getMarketItems(
        currency: Currency,
        sortDescending: Boolean,
        metricsType: MetricsType
    ): Single<List<MarketItem>> {
        return marketKit.marketInfosSingle(250, currency.code, defi = metricsType == MetricsType.DefiCap, apiTag)
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
        chartInterval: HsTimePeriod?,
        sortDescending: Boolean,
        forceRefresh: Boolean
    ): Single<List<TvlModule.MarketTvlItem>> =
        Single.create { emitter ->
            try {
                val defiMarketInfos = defiMarketInfos(currency.code, forceRefresh)
                val marketTvlItems = getMarketTvlItems(defiMarketInfos, currency, chain, chartInterval, sortDescending)
                emitter.onSuccess(marketTvlItems)
            } catch (error: Throwable) {
                emitter.onError(error)
            }
        }

    private fun defiMarketInfos(currencyCode: String, forceRefresh: Boolean): List<DefiMarketInfo> =
        if (forceRefresh || cache.isEmpty()) {
            val defiMarketInfo = marketKit.defiMarketInfosSingle(currencyCode, apiTag).blockingGet()

            cache = defiMarketInfo

            defiMarketInfo
        } else {
            cache
        }

    private fun getMarketTvlItems(
        defiMarketInfoList: List<DefiMarketInfo>,
        currency: Currency,
        chain: TvlModule.Chain,
        chartInterval: HsTimePeriod?,
        sortDescending: Boolean
    ): List<TvlModule.MarketTvlItem> {
        val tvlItems = defiMarketInfoList.map { defiMarketInfo ->
            val diffPercent: BigDecimal? = when (chartInterval) {
                HsTimePeriod.Day1 -> defiMarketInfo.tvlChange1D
                HsTimePeriod.Week1 -> defiMarketInfo.tvlChange1W
                HsTimePeriod.Week2 -> defiMarketInfo.tvlChange2W
                HsTimePeriod.Month1 -> defiMarketInfo.tvlChange1M
                HsTimePeriod.Month3 -> defiMarketInfo.tvlChange3M
                HsTimePeriod.Month6 -> defiMarketInfo.tvlChange6M
                HsTimePeriod.Year1 -> defiMarketInfo.tvlChange1Y
                else -> null
            }
            val diff: CurrencyValue? = diffPercent?.let {
                CurrencyValue(currency, defiMarketInfo.tvl * it.divide(BigDecimal(100)))
            }

            val tvl: BigDecimal = if (chain == TvlModule.Chain.All) {
                defiMarketInfo.tvl
            } else {
                defiMarketInfo.chainTvls[chain.name] ?: BigDecimal.ZERO
            }

            TvlModule.MarketTvlItem(
                defiMarketInfo.fullCoin,
                defiMarketInfo.name,
                defiMarketInfo.chains,
                defiMarketInfo.logoUrl,
                CurrencyValue(currency, tvl),
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

}
