package io.horizontalsystems.chartview.helpers

import android.graphics.RectF
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.Coordinate
import io.horizontalsystems.chartview.Indicator.Candle

object PointConverter {
    fun coordinates(data: ChartData, shape: RectF, verticalPadding: Float): List<Coordinate> {
        val width = shape.width()
        val height = shape.height() - verticalPadding * 2

        val valueMin = data.valueRange.lower
        val valueMax = data.valueRange.upper

        val xRatio = width / (data.endTimestamp - data.startTimestamp)
        val yRatio = height / (valueMax - valueMin)

        val coordinates = mutableListOf<Coordinate>()

        for (item in data.items) {
            val value = item.values[Candle] ?: continue

            val x = (item.timestamp - data.startTimestamp) * xRatio
            val y = (value - valueMin) * yRatio

            coordinates.add(
                Coordinate(
                    x = x,
                    y = shape.height() - verticalPadding - y,
                    item = item,
                )
            )
        }

        return coordinates
    }
}
