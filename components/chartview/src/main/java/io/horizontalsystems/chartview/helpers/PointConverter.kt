package io.horizontalsystems.chartview.helpers

import android.graphics.RectF
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataValueImmutable
import io.horizontalsystems.chartview.Coordinate
import io.horizontalsystems.chartview.Indicator.*
import io.horizontalsystems.chartview.models.ChartPointF
import io.horizontalsystems.chartview.models.PointInfo

object PointConverter {
    fun coordinates(data: ChartData, shape: RectF, verticalPadding: Float): List<Coordinate> {
        val width = shape.width()
        val height = shape.height() - verticalPadding * 2

        val coordinates = mutableListOf<Coordinate>()

        for (item in data.items) {
            val value = item.values[Candle] ?: continue
            val volume = item.values[Volume]
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
}
