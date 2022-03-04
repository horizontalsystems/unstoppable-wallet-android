package io.horizontalsystems.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartPointF

class ChartCurve(private val config: ChartConfig, private val animator: ChartAnimator? = null, override var isVisible: Boolean = false) : ChartDraw {

    private var shape = RectF(0f, 0f, 0f, 0f)
    private var points = listOf<ChartPointF>()

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = config.curveColor
        strokeWidth = config.strokeWidth
        isAntiAlias = true
    }

    fun setPoints(list: List<ChartPointF>) {
        points = list
    }

    fun setShape(rect: RectF) {
        shape = rect
    }

    fun setColor(color: Int) {
        paint.color = color
    }

    override fun draw(canvas: Canvas) {
        if (!isVisible) return
        val path = Path()

        points.forEachIndexed { index, point ->
            when (index) {
                0 -> path.moveTo(point.x, getY(point))
                else -> path.lineTo(point.x, getY(point))
            }
        }

        canvas.drawPath(path, paint)
    }

    private fun getY(point: ChartPointF) : Float {
        return when {
            animator != null -> animator.getAnimatedY(point.y, shape.bottom)
            else -> point.y
        }
    }
}
