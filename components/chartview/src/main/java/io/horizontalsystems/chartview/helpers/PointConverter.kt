package io.horizontalsystems.chartview.helpers

import android.graphics.PointF
import android.graphics.RectF
import io.horizontalsystems.chartview.*
import io.horizontalsystems.chartview.models.ChartPoint
import java.util.*

object PointConverter {
    fun coordinates(data: ChartData, shape: RectF, verticalPadding: Float): List<Coordinate> {
        val width = shape.width()
        val height = shape.height() - verticalPadding * 2

        val coordinates = mutableListOf<Coordinate>()

        for (item in data.items) {
            val value = item.values[Indicator.Candle] ?: continue
            val volume = item.values[Indicator.Volume]
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
            item.values[Indicator.Candle] = ChartData.Value(point.value)
            item.values[Indicator.Volume] = point.volume?.let { ChartData.Value(it) }

            items.add(item)
        }

        val chartData = ChartData(items, startTime, endTime)

        val emaFast = IndicatorHelper.ema(values, 25)
        val emaSlow = IndicatorHelper.ema(values, 50)

        val rsi = IndicatorHelper.rsi(values, 14)
        val (macd, signal, histogram) = IndicatorHelper.macd(values, fastPeriods = 12, slowPeriods = 26, signalPeriods = 9)

        // EMA
        chartData.add(emaFast.map { ChartData.Value(it) }, Indicator.EmaFast)
        chartData.add(emaSlow.map { ChartData.Value(it) }, Indicator.EmaSlow)

        // RSI
        chartData.add(rsi.map { ChartData.Value(it) }, Indicator.Rsi)

        // MACD
        chartData.add(macd.map { ChartData.Value(it) }, Indicator.Macd)
        chartData.add(signal.map { ChartData.Value(it) }, Indicator.MacdSignal)
        chartData.add(histogram.map { ChartData.Value(it) }, Indicator.MacdHistogram)
        addStartPoint(chartData, points, startDayPoint)

        return convertVisible(chartData)
    }

    private fun addStartPoint(chartData: ChartData, points: List<ChartPoint>, startDayPoint: ChartPoint?) {
        if (startDayPoint == null) return
        val list = points.filter { it.timestamp < startDayPoint.timestamp } + startDayPoint
        val prevValues = list.map { it.value }

        val prevEmaFast = IndicatorHelper.ema(prevValues, 25)
        val prevEmaSlow = IndicatorHelper.ema(prevValues, 50)
        val prevRsi = IndicatorHelper.rsi(prevValues, 14)
        val (prevMacd, prevSignal, prevHistogram) = IndicatorHelper.macd(prevValues, fastPeriods = 12, slowPeriods = 26, signalPeriods = 9)

        val item: EnumMap<Indicator, ChartData.Value> = EnumMap(Indicator::class.java)
        item[Indicator.Candle] = ChartData.Value(startDayPoint.value)
        item[Indicator.EmaFast] = ChartData.Value(prevEmaFast.last())
        item[Indicator.EmaSlow] = ChartData.Value(prevEmaSlow.last())
        item[Indicator.Rsi] = ChartData.Value(prevRsi.last())
        item[Indicator.Macd] = ChartData.Value(prevMacd.last())
        item[Indicator.MacdSignal] = ChartData.Value(prevSignal.last())
        item[Indicator.MacdHistogram] = ChartData.Value(prevHistogram.last())

        chartData.insert(ChartData.Item(startDayPoint.timestamp, item))
    }

    private fun convertVisible(chartData: ChartData): ChartData {
        val visibleData = ChartData(mutableListOf(), chartData.startTimestamp, chartData.endTimestamp)

        for (item in chartData.items) {
            if (item.timestamp < visibleData.startTimestamp) {
                continue
            }

            visibleData.items.add(item)

            visibleData.range(item, Indicator.Candle)
            visibleData.range(item, Indicator.Volume)
            visibleData.range(item, Indicator.Macd)
            visibleData.range(item, Indicator.MacdSignal)
            visibleData.range(item, Indicator.MacdHistogram)
        }

        val visibleTimeInterval = visibleData.endTimestamp - visibleData.startTimestamp
        for (item in visibleData.items) {
            val timestamp = item.timestamp - visibleData.startTimestamp
            if (timestamp < 0) {
                continue
            }

            val x = (timestamp.toFloat() / visibleTimeInterval)

            item.setPoint(x, Indicator.Candle, visibleData.valueRange)
            item.setPoint(x, Indicator.Volume, visibleData.volumeRange)
            item.setPoint(x, Indicator.EmaFast, visibleData.valueRange)
            item.setPoint(x, Indicator.EmaSlow, visibleData.valueRange)
            item.setPoint(x, Indicator.Rsi, visibleData.rsiRange)
            item.setPoint(x, Indicator.Macd, visibleData.macdRange)
            item.setPoint(x, Indicator.MacdSignal, visibleData.macdRange)
            item.setPoint(x, Indicator.MacdHistogram, visibleData.histogramRange)
        }

        return visibleData
    }
}
