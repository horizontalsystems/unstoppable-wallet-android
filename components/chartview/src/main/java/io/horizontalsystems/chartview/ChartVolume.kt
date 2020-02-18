package io.horizontalsystems.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartPoint

class ChartVolume(private val config: ChartConfig, private val shape: RectF) {

    private var bars = listOf<VolumeBar>()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun init(points: List<ChartPoint>, startTimestamp: Long, endTimestamp: Long) {
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = config.volumeRectangleColor

        val volumeMax = points.mapNotNull { it.volume }.max() ?: 1f

        val deltaX = shape.width() / (endTimestamp - startTimestamp)
        val deltaY = shape.height() * config.volumeMaximumHeightRatio / volumeMax

        val chunks = mutableListOf<VolumeBar>()

        for (point in points) {
            val volume = point.volume ?: continue

            val y = shape.height() - volume * deltaY
            val x = (point.timestamp - startTimestamp) * deltaX

            chunks.add(VolumeBar(x, y))
        }

        bars = chunks
    }

    fun draw(canvas: Canvas) {
        bars.forEach { bar ->
            val barY = config.getAnimatedY(bar.y, shape.height())
            val barX = bar.x - config.volumeBarWidth
            val rect = RectF(barX, barY, bar.x, shape.height())

            canvas.drawRect(rect, fillPaint)
        }
    }

    class VolumeBar(val x: Float, val y: Float)
}
