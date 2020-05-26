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
}
