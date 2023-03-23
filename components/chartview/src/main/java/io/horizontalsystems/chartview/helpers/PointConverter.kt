package io.horizontalsystems.chartview.helpers

import android.graphics.RectF
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.Coordinate

object PointConverter {
    fun coordinates(data: ChartData, shape: RectF, verticalPadding: Float, horizontalOffset: Float): List<Coordinate> {
        val width = shape.width() - horizontalOffset * 2
        val height = shape.height() - verticalPadding * 2

        val valueMin = data.valueRange.lower
        val valueMax = data.valueRange.upper

        val xRatio = width / (data.endTimestamp - data.startTimestamp)
        val yRatio = height / (valueMax - valueMin)

        val coordinates = mutableListOf<Coordinate>()

        for (item in data.items) {
            val x = (item.timestamp - data.startTimestamp) * xRatio
            val y = (item.value - valueMin) * yRatio

            coordinates.add(
                Coordinate(
                    x = x + horizontalOffset,
                    y = shape.height() - verticalPadding - y,
                    item = item,
                )
            )
        }

        return coordinates
    }
}
