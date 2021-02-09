package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.chartview.*
import io.horizontalsystems.chartview.extensions.ChartInfoTrend
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.chartview.models.MacdInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.lang.Long.max
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
        val price: CurrencyValue,
        val volume: CurrencyValue?,
        val macdInfo: MacdInfo?
)

data class MarketInfoViewItem(
        val rateValue: CurrencyValue,
        val marketCap: CurrencyValue,
        val volume: CurrencyValue,
        val supply: RateChartModule.CoinCodeWithValue,
        val maxSupply: RateChartModule.CoinCodeWithValue?,
        val startDate: String?,
        val website: String?,
        val timestamp: Long
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

        val (emaTrend, rsiTrend, macdTrend) = calculateTrend(chartData, marketInfo)

        return ChartInfoViewItem(chartData, chartType, chartData.diff(), emaTrend, rsiTrend, macdTrend)
    }

    fun createMarketInfo(marketInfo: MarketInfo, currency: Currency, coinCode: String): MarketInfoViewItem {

        return MarketInfoViewItem(
                CurrencyValue(currency, marketInfo.rate),
                CurrencyValue(currency, marketInfo.marketCap ?: BigDecimal.ZERO),
                CurrencyValue(currency, marketInfo.volume),
                RateChartModule.CoinCodeWithValue(coinCode, marketInfo.supply),
                CoinInfoMap.data[coinCode]?.supply?.let { RateChartModule.CoinCodeWithValue(coinCode, it) },
                CoinInfoMap.data[coinCode]?.startDate,
                CoinInfoMap.data[coinCode]?.website,
                marketInfo.timestamp
        )
    }

    private fun calculateTrend(data: ChartData, marketInfo: MarketInfo?): Triple<ChartInfoTrend, ChartInfoTrend, ChartInfoTrend> {
        var emaTrend = ChartInfoTrend.NEUTRAL
        var rsiTrend = ChartInfoTrend.NEUTRAL
        var macdTrend = ChartInfoTrend.NEUTRAL

        if (data.items.isEmpty()) {
            return Triple(emaTrend, rsiTrend, macdTrend)
        }

        var lastItem = data.items.last()

        //  Skip market info item for the trend calculation
        if (data.endTimestamp == marketInfo?.timestamp) {
            lastItem = data.items[data.items.size - 2]
        }

        lastItem.values[Indicator.Rsi]?.let { rsi ->
            rsiTrend = when {
                rsi.value > Indicator.Rsi.max -> ChartInfoTrend.UP
                rsi.value < Indicator.Rsi.min -> ChartInfoTrend.DOWN
                else -> ChartInfoTrend.NEUTRAL
            }
        }

        lastItem.values[Indicator.MacdHistogram]?.let { macd ->
            macdTrend = when {
                macd.value > 0 -> ChartInfoTrend.UP
                macd.value < 0 -> ChartInfoTrend.DOWN
                else -> ChartInfoTrend.NEUTRAL
            }
        }

        val emaSlow = lastItem.values[Indicator.EmaSlow]
        val emaFast = lastItem.values[Indicator.EmaFast]
        if (emaFast != null && emaSlow != null) {
            emaTrend = when {
                emaFast.value > emaSlow.value -> ChartInfoTrend.UP
                emaFast.value < emaSlow.value -> ChartInfoTrend.DOWN
                else -> ChartInfoTrend.NEUTRAL
            }
        }

        return Triple(emaTrend, rsiTrend, macdTrend)
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
