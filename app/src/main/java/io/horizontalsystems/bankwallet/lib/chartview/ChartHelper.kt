package io.horizontalsystems.bankwallet.lib.chartview

import android.graphics.RectF
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.horizontalsystems.bankwallet.lib.chartview.models.DataPoint

class ChartHelper(private val shape: RectF, private val config: ChartConfig) {

    fun setPoints(data: ChartData): List<DataPoint> {
        val width = shape.right
        val height = shape.bottom

        val totalPoints = data.points.size - 1
        val stepX = width / totalPoints
        val stepY = height / (config.valueStep * 4)
        val bottom = config.valueTop - (config.valueStep * 4)

        val scaleSeconds = data.scale * 60
        val points = mutableListOf<DataPoint>()
        var timestamp = data.timestamp - totalPoints * scaleSeconds

        data.points.forEachIndexed { index, value ->
            val x = stepX * index
            val y = height - stepY * (value - bottom)

            points.add(DataPoint(x, y, value, timestamp))
            timestamp += scaleSeconds
        }

        return points
    }
}
