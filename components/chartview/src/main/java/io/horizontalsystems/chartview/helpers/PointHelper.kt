package io.horizontalsystems.chartview.helpers

import android.graphics.RectF
import io.horizontalsystems.chartview.Coordinate
import io.horizontalsystems.chartview.models.ChartPoint
import kotlin.math.max
import kotlin.math.min

object PointHelper {
    fun mapPoints(points: List<ChartPoint>, startTime: Long, endTime: Long, shape: RectF, verticalPadding: Float): List<Coordinate> {
        val (valueLow, valueTop) = mapMinMaxValues(points)

        val height = shape.height()
        val deltaX = (endTime - startTime) / shape.width()
        val deltaY = (valueTop - valueLow) / (height - verticalPadding * 2) // working area without top/bottom padding

        return points.map { point ->
            val x = (point.timestamp - startTime) / deltaX
            val y = (point.value - valueLow) / deltaY

            Coordinate(x, height - verticalPadding - y, point)
        }
    }

    fun mapMinMaxValues(points: List<ChartPoint>): Pair<Float, Float> {
        var minValue = Float.MAX_VALUE
        var maxValue = Float.MIN_VALUE

        for (point in points) {
            minValue = min(point.value, minValue)
            maxValue = max(point.value, maxValue)
        }

        return Pair(minValue, maxValue)
    }

    fun getTopLow(coordinates: List<Coordinate>): Pair<Coordinate, Coordinate> {
        var top = coordinates[0]
        var low = coordinates[0]

        for (item in coordinates) {
            if (item.point.value > top.point.value) {
                top = item
            }

            if (item.point.value < low.point.value) {
                low = item
            }
        }

        return Pair(top, low)
    }
}