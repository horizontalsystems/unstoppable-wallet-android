package io.horizontalsystems.chartview.helpers

import android.graphics.PointF
import android.graphics.RectF
import io.horizontalsystems.chartview.*
import io.horizontalsystems.chartview.Indicator.*
import io.horizontalsystems.chartview.models.ChartPoint

object PointConverter {
    fun coordinates(data: ChartData, shape: RectF, verticalPadding: Float): List<Coordinate> {
        val width = shape.width()
        val height = shape.height() - verticalPadding * 2

        val coordinates = mutableListOf<Coordinate>()

        for (item in data.items) {
            val value = item.values[Candle] ?: continue
            val volume = item.values[Volume]
            val point = value.point
            val x = point.x * width
            val y = point.y * height

            coordinates.add(Coordinate(x, shape.height() - verticalPadding - y, ChartPoint(value.value, volume?.value, item.timestamp)))
        }

        return coordinates
    }

    fun curve(values: List<ChartData.Value>, shape: RectF, verticalPadding: Float): List<PointF> {
        val height = shape.height() - verticalPadding * 2

        return values.map {
            val point = it.point
            val x = point.x * shape.width()
            val y = point.y * height

            PointF(x, shape.height() - verticalPadding - y)
        }
    }

    fun volume(values: List<ChartData.Value>, shape: RectF, topPadding: Float): List<PointF> {
        val height = shape.height() - topPadding

        return values.map {
            val point = it.point
            val x = point.x * shape.width()
            val y = point.y * height

            PointF(x, shape.height() - y)
        }
    }

    fun histogram(values: List<ChartData.Value>, shape: RectF, verticalPadding: Float): List<PointF> {
        val height = shape.height() - verticalPadding * 2

        return values.map {
            val point = it.point
            val x = point.x * shape.width()
            val y = point.y * height

            PointF(x, shape.height() - verticalPadding - y)
        }
    }

    fun convert(points: List<ChartPoint>, startTime: Long, endTime: Long, startDayPoint: ChartPoint?): ChartData {
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
        addStartPoint(chartData, points, startDayPoint)

        return convertVisible(chartData)
    }

    private fun addStartPoint(chartData: ChartData, points: List<ChartPoint>, startDayPoint: ChartPoint?) {
        if (startDayPoint == null) return
        val list = points.filter { it.timestamp < startDayPoint.timestamp } + startDayPoint
        val prevValues = list.map { it.value }

        val prevEmaFast = IndicatorHelper.ema(prevValues, EmaFast.period)
        val prevEmaSlow = IndicatorHelper.ema(prevValues, EmaSlow.period)
        val prevRsi = IndicatorHelper.rsi(prevValues, Rsi.period)
        val (prevMacd, prevSignal, prevHistogram) = IndicatorHelper.macd(prevValues, Macd.fastPeriod, Macd.slowPeriod, Macd.signalPeriod)

        val item = mutableMapOf<Indicator, ChartData.Value?>()
        item[Candle] = ChartData.Value(startDayPoint.value)
        item[EmaFast] = ChartData.Value(prevEmaFast.last())
        item[EmaSlow] = ChartData.Value(prevEmaSlow.last())
        item[Rsi] = ChartData.Value(prevRsi.last())
        item[Macd] = ChartData.Value(prevMacd.last())
        item[MacdSignal] = ChartData.Value(prevSignal.last())
        item[MacdHistogram] = ChartData.Value(prevHistogram.last())

        chartData.insert(ChartData.Item(startDayPoint.timestamp, item))
    }

    private fun convertVisible(chartData: ChartData): ChartData {
        val visibleData = ChartData(mutableListOf(), chartData.startTimestamp, chartData.endTimestamp)

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
