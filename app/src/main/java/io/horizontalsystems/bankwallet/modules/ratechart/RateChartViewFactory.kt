package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.chartview.*
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal

data class ChartInfoViewItem(
        val chartData: ChartData,
        val chartType: ChartView.ChartType,
        val diffValue: BigDecimal
)

data class ChartPointViewItem(
        val date: Long,
        val currencyValue: CurrencyValue,
        val volume: CurrencyValue?,
        val chartType: ChartType
)

data class MarketInfoViewItem(
        val rateValue: CurrencyValue,
        val marketCap: CurrencyValue,
        val volume: CurrencyValue,
        val supply: RateChartModule.CoinCodeWithValue,
        val maxSupply: RateChartModule.CoinCodeWithValue?,
        val timestamp: Long
)

class RateChartViewFactory {
    fun createChartInfo(type: ChartType, chartInfo: ChartInfo, marketInfo: MarketInfo?): ChartInfoViewItem {
        val chartData = prepareChartData(chartInfo, marketInfo, type)
        val chartType = when (type) {
            ChartType.DAILY -> ChartView.ChartType.DAILY
            ChartType.WEEKLY -> ChartView.ChartType.WEEKLY
            ChartType.MONTHLY -> ChartView.ChartType.MONTHLY
            ChartType.MONTHLY3 -> ChartView.ChartType.MONTHLY3
            ChartType.MONTHLY6 -> ChartView.ChartType.MONTHLY6
            ChartType.MONTHLY12 -> ChartView.ChartType.MONTHLY12
            ChartType.MONTHLY24 -> ChartView.ChartType.MONTHLY24
        }

        return ChartInfoViewItem(chartData, chartType, chartData.diff())
    }

    fun createMarketInfo(marketInfo: MarketInfo, currency: Currency, coinCode: String): MarketInfoViewItem {
        return MarketInfoViewItem(
                CurrencyValue(currency, marketInfo.rate),
                CurrencyValue(currency, marketInfo.marketCap.toBigDecimal()),
                CurrencyValue(currency, marketInfo.volume.toBigDecimal()),
                RateChartModule.CoinCodeWithValue(coinCode, marketInfo.supply.toBigDecimal()),
                MaxSupplyMap.maxSupplies[coinCode]?.let { RateChartModule.CoinCodeWithValue(coinCode, it) },
                marketInfo.timestamp
        )
    }

    private fun prepareChartData(chartInfo: ChartInfo, marketInfo: MarketInfo?, chartType: ChartType): ChartData {
        val points = chartInfo.points.map { ChartPoint(it.value.toFloat(), it.volume?.toFloat(), it.timestamp) }
        var startTime = chartInfo.startTimestamp

        if (marketInfo == null) {
            return ChartDataFactory.build(points, startTime, chartInfo.endTimestamp, startDayPoint = null)
        }

        val pointsWithMarketPrice = points + ChartPoint(marketInfo.rate.toFloat(), null, marketInfo.timestamp)
        var startDayPoint: ChartPoint? = null

        if (chartType == ChartType.DAILY) {
            startTime = marketInfo.timestamp - chartType.rangeInterval
            startDayPoint = ChartPoint(marketInfo.rateOpen24Hour.toFloat(), null, startTime)
        }

        return ChartDataFactory.build(pointsWithMarketPrice, startTime, marketInfo.timestamp, startDayPoint)
    }
}
