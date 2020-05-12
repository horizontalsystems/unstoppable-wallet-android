package io.horizontalsystems.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartConfig

class ChartVolume(private val config: ChartConfig, private val animator: ChartAnimator) : ChartDraw {

    private var shape = RectF(0f, 0f, 0f, 0f)

    private var bars = listOf<VolumeBar>()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = config.volumeRectangleColor
    }

    fun setShape(rect: RectF) {
        shape = rect
    }

    fun setBars(volumeBars: List<VolumeBar>) {
        bars = volumeBars
    }

    override fun draw(canvas: Canvas) {
        bars.forEach { bar ->
            val barY = animator.getAnimatedY(bar.y, shape.height())
            val barX = bar.x - config.volumeBarWidth
            val rect = RectF(barX, barY, bar.x, shape.height())

            canvas.drawRect(rect, paint)
        }
    }

    class VolumeBar(val x: Float, val y: Float)
}
