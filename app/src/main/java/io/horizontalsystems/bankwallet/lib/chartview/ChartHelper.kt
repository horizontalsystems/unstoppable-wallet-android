package io.horizontalsystems.bankwallet.lib.chartview

import android.graphics.RectF
import io.horizontalsystems.bankwallet.lib.chartview.ChartCurve.Coordinate
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint

class ChartHelper(private val shape: RectF, private val config: ChartConfig) {

    fun setCoordinates(points: List<ChartPoint>, startTimestamp: Long, endTimestamp: Long): List<Coordinate> {
        val width = shape.width()
        val height = shape.height()

        val deltaX = (endTimestamp - startTimestamp) / width
        val deltaY = (config.valueMax - config.valueMin) / height

        return points.map { point ->
            val x = (point.timestamp - startTimestamp) / deltaX
            val y = (point.value - config.valueMin) / deltaY

            Coordinate(x, height - y, point)
        }
    }

    fun getTopAndLow(coordinates: List<Coordinate>): Pair<Coordinate, Coordinate> {
        var topCoordinate = coordinates[0]
        var lowCoordinate = coordinates[0]

        for (coordinate in coordinates) {
            if (coordinate.point.value > topCoordinate.point.value) {
                topCoordinate = coordinate
            }

            if (coordinate.point.value < lowCoordinate.point.value) {
                lowCoordinate = coordinate
            }
        }

        return Pair(topCoordinate, lowCoordinate)
    }

    companion object {
        fun convert(points: List<Float>, scaleMinutes: Int, lastTimestamp: Long): List<ChartPoint> {

            val scaleSecs = scaleMinutes * 60
            var timestamp = lastTimestamp

            val chartPoints = mutableListOf<ChartPoint>()

            for (i in (points.size - 1) downTo 0) {
                chartPoints.add(0, ChartPoint(points[i], timestamp))
                timestamp -= scaleSecs
            }

            return chartPoints
        }
    }
}
