package io.horizontalsystems.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartPointF
import java.lang.Float.max

class ChartHistogram(private val config: ChartConfig, private val animator: ChartAnimator, override var isVisible: Boolean = false) : ChartDraw {

    private var shape = RectF(0f, 0f, 0f, 0f)
    private var bars = listOf<ChartPointF>()
    private var volumeWidth = 0f

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

    fun setPoints(points: List<ChartPointF>) {
        bars = points

        if (bars.isNotEmpty()) {
            var minDiff = config.volumeWidth
            for (i in 0 until (bars.size - 1)) {
                val diff = bars[i + 1].x - bars[i].x - 1 // 1 is horizontal space between bars
                if (diff < minDiff) {
                    minDiff = diff
                }
            }

            volumeWidth = max(minDiff, 1f)
        }
    }

    override fun draw(canvas: Canvas) {
        if (!isVisible || bars.isEmpty()) return

        val middle = shape.height() / 2

        val halfWidth = volumeWidth / 2
        bars.forEach { bar ->
            val barTop = animator.getAnimatedY(bar.y, middle)
            val barStart = bar.x - halfWidth
            val barEnd = bar.x + halfWidth

            val paint = if (bar.y > middle) {
                paintDown
            } else {
                paintUp
            }

            canvas.drawRect(RectF(barStart, barTop, barEnd, middle), paint)
        }
    }
}
