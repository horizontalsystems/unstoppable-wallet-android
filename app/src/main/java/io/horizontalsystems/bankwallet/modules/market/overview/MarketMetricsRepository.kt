package io.horizontalsystems.bankwallet.modules.market.overview

import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetricsItem
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetricsPoint
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.GlobalMarketPoint
import io.horizontalsystems.marketkit.models.TimePeriod
import io.reactivex.Single
import java.math.BigDecimal

class MarketMetricsRepository(
    private val marketKit: MarketKit
) {
    private var marketMetricsItemCache: MarketMetricsItem? = null

    fun get(baseCurrency: Currency, forceRefresh: Boolean): Single<MarketMetricsItem> =
        if (forceRefresh || marketMetricsItemCache == null) {
            marketKit.globalMarketPointsSingle(baseCurrency.code, TimePeriod.Hour24)
                .map {
                    marketMetricsItemCache = marketMetricsItem(it, baseCurrency)
                    marketMetricsItemCache
                }
        } else {
            Single.just(marketMetricsItemCache)
        }

    private fun marketMetricsItem(
        globalMarketPoints: List<GlobalMarketPoint>,
        baseCurrency: Currency
    ): MarketMetricsItem {
        var marketCap = BigDecimal.ZERO
        var marketCapDiff = BigDecimal.ZERO
        var defiMarketCap = BigDecimal.ZERO
        var defiMarketCapDiff = BigDecimal.ZERO
        var volume24h = BigDecimal.ZERO
        var volume24hDiff = BigDecimal.ZERO
        var btcDominance = BigDecimal.ZERO
        var btcDominanceDiff = BigDecimal.ZERO
        var tvl = BigDecimal.ZERO
        var tvlDiff = BigDecimal.ZERO

        if (globalMarketPoints.isNotEmpty()) {
            val startingPoint = globalMarketPoints.first()
            val endingPoint = globalMarketPoints.last()

            marketCap = endingPoint.marketCap
            marketCapDiff = diff(startingPoint.marketCap, marketCap)

            defiMarketCap = endingPoint.defiMarketCap
            defiMarketCapDiff = diff(startingPoint.defiMarketCap, defiMarketCap)

            volume24h = endingPoint.volume24h
            volume24hDiff = diff(startingPoint.volume24h, volume24h)

            btcDominance = endingPoint.btcDominance
            btcDominanceDiff = diff(startingPoint.btcDominance, btcDominance)

            tvl = endingPoint.tvl
            tvlDiff = diff(startingPoint.tvl, tvl)
        }

        return MarketMetricsItem(
            baseCurrency.code,
            CurrencyValue(baseCurrency, volume24h),
            volume24hDiff,
            CurrencyValue(baseCurrency, marketCap),
            marketCapDiff,
            btcDominance,
            btcDominanceDiff,
            CurrencyValue(baseCurrency, defiMarketCap),
            defiMarketCapDiff,
            CurrencyValue(baseCurrency, tvl),
            tvlDiff,
            totalMarketCapPoints = globalMarketPoints.map { MarketMetricsPoint(it.marketCap, it.timestamp) },
            btcDominancePoints = globalMarketPoints.map { MarketMetricsPoint(it.btcDominance, it.timestamp) },
            volume24Points = globalMarketPoints.map { MarketMetricsPoint(it.volume24h, it.timestamp) },
            defiMarketCapPoints = globalMarketPoints.map { MarketMetricsPoint(it.defiMarketCap, it.timestamp) },
            defiTvlPoints = globalMarketPoints.map { MarketMetricsPoint(it.tvl, it.timestamp) }
        )
    }

    private fun diff(sourceValue: BigDecimal, targetValue: BigDecimal): BigDecimal =
        if (sourceValue.compareTo(BigDecimal.ZERO) != 0)
            ((targetValue - sourceValue) * BigDecimal(100)) / sourceValue
        else BigDecimal.ZERO
}
