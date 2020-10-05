package io.horizontalsystems.chartview

import io.horizontalsystems.chartview.Indicator.*
import io.horizontalsystems.chartview.helpers.IndicatorHelper
import io.horizontalsystems.chartview.models.ChartPoint

object ChartDataFactory {
    fun build(points: List<ChartPoint>, startTime: Long, endTime: Long, isExpired: Boolean): ChartData {
        val values = mutableListOf<Float>()
        val items = mutableListOf<ChartData.Item>()

        for (point in points) {
            values.add(point.value)

            val item = ChartData.Item(point.timestamp)
            item.values[Candle] = ChartData.Value(point.value)
            item.values[Volume] = point.volume?.let { ChartData.Value(it) }

            items.add(item)
        }

        val chartData = ChartData(items, startTime, endTime)

        val emaFast = IndicatorHelper.ema(values, EmaFast.period)
        val emaSlow = IndicatorHelper.ema(values, EmaSlow.period)

        val rsi = IndicatorHelper.rsi(values, Rsi.period)
        val (macd, signal, histogram) = IndicatorHelper.macd(values, Macd.fastPeriod, Macd.slowPeriod, Macd.signalPeriod)

        // EMA
        chartData.add(emaFast.map { ChartData.Value(it) }, EmaFast)
        chartData.add(emaSlow.map { ChartData.Value(it) }, EmaSlow)

        // RSI
        chartData.add(rsi.map { ChartData.Value(it) }, Rsi)

        // MACD
        chartData.add(macd.map { ChartData.Value(it) }, Macd)
        chartData.add(signal.map { ChartData.Value(it) }, MacdSignal)
        chartData.add(histogram.map { ChartData.Value(it) }, MacdHistogram)

        return convertPoints(chartData, isExpired)
    }

    private fun convertPoints(chartData: ChartData, isExpired: Boolean): ChartData {
        val visibleData = ChartData(mutableListOf(), chartData.startTimestamp, chartData.endTimestamp, isExpired)

        for (item in chartData.items) {
            if (item.timestamp < visibleData.startTimestamp) {
                continue
            }

            visibleData.items.add(item)

            visibleData.range(item, Candle)
            visibleData.range(item, Volume)
            visibleData.range(item, Macd)
            visibleData.range(item, MacdSignal)
            visibleData.range(item, MacdHistogram)
        }

        val visibleTimeInterval = visibleData.endTimestamp - visibleData.startTimestamp
        for (item in visibleData.items) {
            val timestamp = item.timestamp - visibleData.startTimestamp
            if (timestamp < 0) {
                continue
            }

            val x = (timestamp.toFloat() / visibleTimeInterval)

            item.setPoint(x, Candle, visibleData.valueRange)
            item.setPoint(x, Volume, visibleData.volumeRange)
            item.setPoint(x, EmaFast, visibleData.valueRange)
            item.setPoint(x, EmaSlow, visibleData.valueRange)
            item.setPoint(x, Rsi, visibleData.rsiRange)
            item.setPoint(x, Macd, visibleData.macdRange)
            item.setPoint(x, MacdSignal, visibleData.macdRange)
            item.setPoint(x, MacdHistogram, visibleData.histogramRange)
        }

        return visibleData
    }
}
