package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.chartview.*
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.chartview.models.MacdInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.*
import java.lang.Long.max
import java.math.BigDecimal

data class ChartInfoViewItem(
        val chartData: ChartData,
        val chartType: ChartView.ChartType,
        val diffValue: BigDecimal
)

data class ChartPointViewItem(
        val date: Long,
        val price: CurrencyValue,
        val volume: CurrencyValue?,
        val macdInfo: MacdInfo?
)

data class MarketInfoViewItem(
    val currency: Currency,
    val rateValue: BigDecimal,
    val marketCap: BigDecimal,
    val circulatingSupply: RateChartModule.CoinCodeWithValue,
    val totalSupply: RateChartModule.CoinCodeWithValue,
    val timestamp: Long,
    val rateHigh24h: BigDecimal,
    val rateLow24h: BigDecimal,
    val volume24h: BigDecimal,
    val marketCapDiff24h: BigDecimal,
    val coinInfo: CoinInfo,
    val rateDiffs: Map<TimePeriod, Map<String, BigDecimal>>
)

class RateChartViewFactory {
    fun createChartInfo(type: ChartType, chartInfo: ChartInfo, marketInfo: MarketInfo?): ChartInfoViewItem {
        val chartData = createChartData(chartInfo, marketInfo)
        val chartType = when (type) {
            ChartType.TODAY -> ChartView.ChartType.TODAY
            ChartType.DAILY -> ChartView.ChartType.DAILY
            ChartType.WEEKLY -> ChartView.ChartType.WEEKLY
            ChartType.WEEKLY2 -> ChartView.ChartType.WEEKLY2
            ChartType.MONTHLY -> ChartView.ChartType.MONTHLY
            ChartType.MONTHLY3 -> ChartView.ChartType.MONTHLY3
            ChartType.MONTHLY6 -> ChartView.ChartType.MONTHLY6
            ChartType.MONTHLY12 -> ChartView.ChartType.MONTHLY12
            ChartType.MONTHLY24 -> ChartView.ChartType.MONTHLY24
        }

        return ChartInfoViewItem(chartData, chartType, chartData.diff())
    }

    fun createMarketInfo(marketInfo: MarketInfo, coinMarket: CoinMarketDetails, currency: Currency, coinCode: String): MarketInfoViewItem {
        val rateHigh24h = if (marketInfo.rate > coinMarket.rateHigh24h) {
            marketInfo.rate
        } else {
            coinMarket.rateHigh24h
        }

        val rateLow24h = if (marketInfo.rate < coinMarket.rateLow24h) {
            marketInfo.rate
        } else {
            coinMarket.rateLow24h
        }

        return MarketInfoViewItem(
            currency = currency,
            rateValue = marketInfo.rate,
            marketCap = coinMarket.marketCap,
            circulatingSupply = RateChartModule.CoinCodeWithValue(coinCode, coinMarket.circulatingSupply),
            totalSupply = RateChartModule.CoinCodeWithValue(coinCode, coinMarket.totalSupply),
            timestamp = marketInfo.timestamp,
            rateHigh24h = rateHigh24h,
            rateLow24h = rateLow24h,
            volume24h = coinMarket.volume24h,
            marketCapDiff24h = coinMarket.marketCapDiff24h,
            coinInfo = coinMarket.coinInfo,
            rateDiffs = coinMarket.rateDiffs
        )
    }

    private fun createChartData(chartInfo: ChartInfo, marketInfo: MarketInfo?): ChartData {
        val points = chartInfo.points.map { ChartPoint(it.value.toFloat(), it.volume?.toFloat(), it.timestamp) }.toMutableList()
        val lastPoint = chartInfo.points.lastOrNull()

        var endTimestamp = chartInfo.endTimestamp
        val lastPointTimestamp = lastPoint?.timestamp

        if (marketInfo != null && lastPointTimestamp != null && marketInfo.timestamp > lastPointTimestamp) {
            endTimestamp = max(marketInfo.timestamp, endTimestamp)
            points.add(ChartPoint(marketInfo.rate.toFloat(), null, marketInfo.timestamp))
        }

        return ChartDataFactory.build(points, chartInfo.startTimestamp, endTimestamp, chartInfo.isExpired)
    }
}
