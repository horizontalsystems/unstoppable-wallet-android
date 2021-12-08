package io.horizontalsystems.chartview

import io.horizontalsystems.chartview.Indicator.*
import io.horizontalsystems.chartview.helpers.IndicatorHelper
import io.horizontalsystems.chartview.models.ChartPoint

object ChartDataFactory {
    fun build(points: List<ChartPoint>, startTime: Long, endTime: Long, isExpired: Boolean): ChartData {
        val values = mutableListOf<Float>()
        val items = mutableListOf<ChartDataItem>()

        for (point in points) {
            values.add(point.value)

            val item = ChartDataItem(point.timestamp)
            item.values[Candle] = ChartDataValue(point.value)
            item.values[Volume] = point.volume?.let { ChartDataValue(it) }

            items.add(item)
        }

        val chartDataBuilder = ChartDataBuilder(items, startTime, endTime)

        val emaFast = IndicatorHelper.ema(values, EmaFast.period)
        val emaSlow = IndicatorHelper.ema(values, EmaSlow.period)

        val rsi = IndicatorHelper.rsi(values, Rsi.period)
        val (macd, signal, histogram) = IndicatorHelper.macd(values, Macd.fastPeriod, Macd.slowPeriod, Macd.signalPeriod)

        // EMA
        chartDataBuilder.add(emaFast.map { ChartDataValue(it) }, EmaFast)
        chartDataBuilder.add(emaSlow.map { ChartDataValue(it) }, EmaSlow)

        // RSI
        chartDataBuilder.add(rsi.map { ChartDataValue(it) }, Rsi)

        // MACD
        chartDataBuilder.add(macd.map { ChartDataValue(it) }, Macd)
        chartDataBuilder.add(signal.map { ChartDataValue(it) }, MacdSignal)
        chartDataBuilder.add(histogram.map { ChartDataValue(it) }, MacdHistogram)

        return convertPoints(chartDataBuilder, isExpired)
    }

    private fun convertPoints(chartDataBuilder: ChartDataBuilder, isExpired: Boolean): ChartData {
        val visibleChartDataBuilder = ChartDataBuilder(mutableListOf(), chartDataBuilder.startTimestamp, chartDataBuilder.endTimestamp, isExpired)

        for (item in chartDataBuilder.items) {
            if (item.timestamp < visibleChartDataBuilder.startTimestamp) {
                continue
            }

            visibleChartDataBuilder.items.add(item)

            visibleChartDataBuilder.range(item, Candle)
            visibleChartDataBuilder.range(item, Volume)
            visibleChartDataBuilder.range(item, Macd)
            visibleChartDataBuilder.range(item, MacdSignal)
            visibleChartDataBuilder.range(item, MacdHistogram)
            visibleChartDataBuilder.range(item, Dominance)
        }

        val visibleTimeInterval = visibleChartDataBuilder.endTimestamp - visibleChartDataBuilder.startTimestamp
        for (item in visibleChartDataBuilder.items) {
            val timestamp = item.timestamp - visibleChartDataBuilder.startTimestamp
            if (timestamp < 0) {
                continue
            }

            val x = (timestamp.toFloat() / visibleTimeInterval)

            item.setPoint(x, Candle, visibleChartDataBuilder.valueRange)
            item.setPoint(x, Volume, visibleChartDataBuilder.volumeRange)
            item.setPoint(x, EmaFast, visibleChartDataBuilder.valueRange)
            item.setPoint(x, EmaSlow, visibleChartDataBuilder.valueRange)
            item.setPoint(x, Rsi, visibleChartDataBuilder.rsiRange)
            item.setPoint(x, Macd, visibleChartDataBuilder.macdRange)
            item.setPoint(x, MacdSignal, visibleChartDataBuilder.macdRange)
            item.setPoint(x, MacdHistogram, visibleChartDataBuilder.histogramRange)
            item.setPoint(x, Dominance, visibleChartDataBuilder.dominanceRange)
        }

        return visibleChartDataBuilder.build()
    }
}
