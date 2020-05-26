package io.horizontalsystems.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartConfig

class ChartHistogram(private val config: ChartConfig, private val animator: ChartAnimator, override var isVisible: Boolean = false) : ChartDraw {

    private var shape = RectF(0f, 0f, 0f, 0f)

    private var bars = listOf<PointF>()

    private val paintUp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = config.macdHistogramUpColor
    }

    private val paintDown = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = config.macdHistogramDownColor
    }

    fun setShape(rect: RectF) {
        shape = rect
    }

    fun setPoints(points: List<PointF>) {
        bars = points
    }

    override fun draw(canvas: Canvas) {
        if (!isVisible) return

        val middle = shape.height() / 2
        var prevEnd = -100f

        bars.forEach { bar ->
            val barTop = animator.getAnimatedY(bar.y, middle)
            var barStart = bar.x - config.volumeWidth
            var barEnd = bar.x

            if (prevEnd > barStart) {
                barStart = bar.x
                barEnd = bar.x + config.volumeWidth
            }

            val paint = if (bar.y > middle) {
                paintDown
            } else {
                paintUp
            }

            prevEnd = barEnd

            canvas.drawRect(RectF(barStart, barTop, barEnd, middle), paint)
        }
    }
}
