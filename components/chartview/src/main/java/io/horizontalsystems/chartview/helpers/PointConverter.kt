package io.horizontalsystems.chartview.helpers

import android.graphics.PointF
import android.graphics.RectF
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.Coordinate
import io.horizontalsystems.chartview.Indicator.*
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
                    )
                )
            )
        }

        return coordinates
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

    fun curve(values: List<ChartData.Value>, shape: RectF, verticalPadding: Float): List<PointF> {
        //use padding both for top and bottom
        val height = shape.height() - verticalPadding * 2
        return getPoints(values, shape, height, verticalPadding)
    }

    fun curveForMinimal(values: List<ChartData.Value>, shape: RectF, verticalPadding: Float): List<PointF> {
        //use padding only for bottom side
        val height = shape.height() - verticalPadding
        return getPoints(values, shape, height, verticalPadding)
    }

    fun histogram(values: List<ChartData.Value>, shape: RectF, verticalPadding: Float): List<PointF> {
        val height = shape.height() - verticalPadding * 2
        return getPoints(values, shape, height, verticalPadding)
    }

    private fun getPoints(values: List<ChartData.Value>, shape: RectF, height: Float, verticalPadding: Float): List<PointF> {
        return values.map {
            val point = it.point
            val x = point.x * shape.width()
            val y = point.y * height

            PointF(x, shape.height() - verticalPadding - y)
        }
    }
}
