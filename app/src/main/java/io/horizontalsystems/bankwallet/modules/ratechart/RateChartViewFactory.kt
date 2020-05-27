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
        val diffValue: BigDecimal,
        val emaTrend: ChartInfoTrend,
        val rsiTrend: ChartInfoTrend,
        val macdTrend: ChartInfoTrend
)

data class ChartPointViewItem(
        val date: Long,
        val currencyValue: CurrencyValue,
        val volume: CurrencyValue?,
        val chartType: ChartType
)

enum class ChartInfoTrend {
    UP, DOWN, NEUTRAL
}

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
        val chartData = createChartData(chartInfo, marketInfo, type)
        val chartType = when (type) {
            ChartType.DAILY -> ChartView.ChartType.DAILY
            ChartType.WEEKLY -> ChartView.ChartType.WEEKLY
            ChartType.MONTHLY -> ChartView.ChartType.MONTHLY
            ChartType.MONTHLY3 -> ChartView.ChartType.MONTHLY3
            ChartType.MONTHLY6 -> ChartView.ChartType.MONTHLY6
            ChartType.MONTHLY12 -> ChartView.ChartType.MONTHLY12
            ChartType.MONTHLY24 -> ChartView.ChartType.MONTHLY24
        }

        val (emaTrend, rsiTrend, macdTrend) = calculateTrend(chartData.items.lastOrNull())

        return ChartInfoViewItem(chartData, chartType, chartData.diff(), emaTrend, rsiTrend, macdTrend)
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

    private fun calculateTrend(data: ChartData.Item?): Triple<ChartInfoTrend, ChartInfoTrend, ChartInfoTrend> {
        var emaTrend = ChartInfoTrend.NEUTRAL
        var rsiTrend = ChartInfoTrend.NEUTRAL
        var macdTrend = ChartInfoTrend.NEUTRAL

        if (data == null) {
            return Triple(emaTrend, rsiTrend, macdTrend)
        }

        data.values[Indicator.Rsi]?.let { rsi ->
            rsiTrend = when {
                rsi.value > Indicator.Rsi.max -> ChartInfoTrend.UP
                rsi.value < Indicator.Rsi.min -> ChartInfoTrend.DOWN
                else -> ChartInfoTrend.NEUTRAL
            }
        }

        data.values[Indicator.MacdHistogram]?.let { macd ->
            macdTrend = when {
                macd.value > 0 -> ChartInfoTrend.UP
                macd.value < 0 -> ChartInfoTrend.DOWN
                else -> ChartInfoTrend.NEUTRAL
            }
        }

        val emaSlow = data.values[Indicator.EmaSlow]
        val emaFast = data.values[Indicator.EmaFast]
        if (emaFast != null && emaSlow != null) {
            emaTrend = when {
                emaFast.value > emaSlow.value -> ChartInfoTrend.UP
                emaFast.value < emaSlow.value -> ChartInfoTrend.DOWN
                else -> ChartInfoTrend.NEUTRAL
            }
        }

        return Triple(emaTrend, rsiTrend, macdTrend)
    }

    private fun createChartData(chartInfo: ChartInfo, marketInfo: MarketInfo?, chartType: ChartType): ChartData {
        val points = chartInfo.points.map { ChartPoint(it.value.toFloat(), it.volume?.toFloat(), it.timestamp) }
        var startTime = chartInfo.startTimestamp
        val lastPoint = chartInfo.points.lastOrNull()

        //  Points expired or not market info
        if (lastPoint?.timestamp != chartInfo.endTimestamp || marketInfo == null) {
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
