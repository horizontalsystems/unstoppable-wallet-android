package io.horizontalsystems.chartview

import io.horizontalsystems.chartview.models.ChartPointF

class PointsManager(
    private val xMax: Float,
    private val yMax: Float,
    private val curveVerticalOffset: Float,
) {
    fun getPoints(
        valuesByTimestamp: LinkedHashMap<Long, Float>,
        minTimestamp: Long,
        maxTimestamp: Long,
        minValue: Float,
        maxValue: Float,
    ): List<ChartPointF> {
        // timestamp = ax + startTimestamp
        // x = (timestamp - startTimestamp) / a
        // a = (timestamp - startTimestamp) / x
        val xRatio = (maxTimestamp - minTimestamp) / xMax

        // value = ay + minValue
        // y = (value - minValue) / a
        // a = (value - minValue) / y
        val yRatio = (maxValue - minValue) / (yMax - 2 * curveVerticalOffset)

        return valuesByTimestamp.map { (timestamp, value) ->
            val x = (timestamp - minTimestamp) / xRatio
            val y = (value - minValue) / yRatio + curveVerticalOffset

            val y2 = (y * -1) + yMax

            ChartPointF(x, y2)
        }
    }
}
