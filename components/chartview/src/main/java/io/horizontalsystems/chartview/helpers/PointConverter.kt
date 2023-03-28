package io.horizontalsystems.chartview.helpers

import android.graphics.RectF
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.Coordinate

object PointConverter {
    fun coordinates(data: ChartData, shape: RectF, verticalPadding: Float, horizontalOffset: Float): List<Coordinate> {
        val width = shape.width() - horizontalOffset * 2
        val height = shape.height() - verticalPadding * 2

        var valueMin = data.valueRange.lower
        var valueMax = data.valueRange.upper

        if (valueMin == valueMax) {
            valueMin *= 0.9f
            valueMax *= 1.1f
        }


        var toStartTimestamp = data.startTimestamp
        var toEndTimestamp = data.endTimestamp

        if (toStartTimestamp == toEndTimestamp) {
            toStartTimestamp = (toStartTimestamp * 0.9).toLong()
            toEndTimestamp = (toEndTimestamp * 1.1).toLong()
        }


        val xRatio = width / (toEndTimestamp - toStartTimestamp)
        val yRatio = height / (valueMax - valueMin)

        val coordinates = mutableListOf<Coordinate>()

        for (item in data.items) {
            val x = (item.timestamp - toStartTimestamp) * xRatio
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
