package io.horizontalsystems.chartview.helpers

import android.graphics.RectF
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataValueImmutable
import io.horizontalsystems.chartview.Coordinate
import io.horizontalsystems.chartview.Indicator
import io.horizontalsystems.chartview.Indicator.*
import io.horizontalsystems.chartview.models.ChartPointF
import io.horizontalsystems.chartview.models.MacdInfo
import io.horizontalsystems.chartview.models.PointInfo

object PointConverter {
    fun coordinates(data: ChartData, shape: RectF, verticalPadding: Float): List<Coordinate> {
        val width = shape.width()
        val height = shape.height() - verticalPadding * 2

        val coordinates = mutableListOf<Coordinate>()

        for (item in data.items) {
            val value = item.values[Candle] ?: continue
            val volume = item.values[Volume]
            val macd = item.values[Macd]
            val signal = item.values[MacdSignal]
            val histogram = item.values[MacdHistogram]
            val dominance = item.values[Dominance]

            val point = value.point
            val x = point.x * width
            val y = point.y * height

            coordinates.add(
                Coordinate(
                    x = x,
                    y = shape.height() - verticalPadding - y,
                    point = PointInfo(
                        value.value,
                        volume?.value,
                        MacdInfo(macd?.value, signal?.value, histogram?.value),
                        dominance?.value?.toBigDecimal(),
                        item.timestamp
                    ),
                    item = item,
                )
            )
        }

        return coordinates
    }

    fun volume(values: List<ChartDataValueImmutable>, shape: RectF, topPadding: Float): List<ChartPointF> {
        val height = shape.height() - topPadding

        return values.map {
            val point = it.point
            val x = point.x * shape.width()
            val y = point.y * height

            ChartPointF(x, shape.height() - y)
        }
    }

    fun curve(values: List<ChartDataValueImmutable>, shape: RectF, verticalPadding: Float): List<ChartPointF> {
        //use padding both for top and bottom
        val height = shape.height() - verticalPadding * 2
        return getPoints(values, shape, height, verticalPadding)
    }

    fun curveMap(chartData: ChartData, indicator: Indicator, shape: RectF, verticalPadding: Float): LinkedHashMap<Long, ChartPointF> {
        //use padding both for top and bottom
        val height = shape.height() - verticalPadding * 2

        return getPointsMap(chartData.valuesMap(indicator), shape, height, verticalPadding)
    }

    fun curveForMinimal(values: List<ChartDataValueImmutable>, shape: RectF, verticalPadding: Float): List<ChartPointF> {
        //use padding only for bottom side
        val height = shape.height() - verticalPadding
        return getPoints(values, shape, height, verticalPadding)
    }

    fun histogram(values: List<ChartDataValueImmutable>, shape: RectF, verticalPadding: Float): List<ChartPointF> {
        val height = shape.height() - verticalPadding * 2
        return getPoints(values, shape, height, verticalPadding)
    }

    private fun getPoints(values: List<ChartDataValueImmutable>, shape: RectF, height: Float, verticalPadding: Float): List<ChartPointF> {
        return values.map {
            val point = it.point
            val x = point.x * shape.width()
            val y = point.y * height

            ChartPointF(x, shape.height() - verticalPadding - y)
        }
    }

    private fun getPointsMap(values: LinkedHashMap<Long, ChartDataValueImmutable>, shape: RectF, height: Float, verticalPadding: Float): LinkedHashMap<Long, ChartPointF> {
        return LinkedHashMap(values.map { (timestamp, it) ->
            val point = it.point
            val x = point.x * shape.width()
            val y = point.y * height

            timestamp to ChartPointF(x, shape.height() - verticalPadding - y)
        }.toMap()
        )
    }
}
