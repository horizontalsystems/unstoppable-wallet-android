package io.horizontalsystems.chartview.helpers

import android.graphics.RectF
import io.horizontalsystems.chartview.ChartVolume
import io.horizontalsystems.chartview.models.ChartPoint

object VolumeHelper {
    fun mapPoints(points: List<ChartPoint>, startTime: Long, endTime: Long, shape: RectF, maxHeightRatio: Float): List<ChartVolume.VolumeBar> {
        val volumeMax = points.mapNotNull { it.volume }.max() ?: 1f

        val deltaX = shape.width() / (endTime - startTime)
        val deltaY = shape.height() * maxHeightRatio / volumeMax

        val chunks = mutableListOf<ChartVolume.VolumeBar>()

        for (point in points) {
            val volume = point.volume ?: continue

            val y = shape.height() - volume * deltaY
            val x = (point.timestamp - startTime) * deltaX

            chunks.add(ChartVolume.VolumeBar(x, y))
        }

        return chunks
    }
}