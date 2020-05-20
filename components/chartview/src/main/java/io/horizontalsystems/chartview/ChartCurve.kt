package io.horizontalsystems.chartview

import android.graphics.*
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartConfig

class ChartCurve(private val config: ChartConfig, private val animator: ChartAnimator) : ChartDraw {

    private var shape = RectF(0f, 0f, 0f, 0f)
    private var points = listOf<PointF>()

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = config.curveColor
        strokeWidth = config.strokeWidth
        isAntiAlias = true
    }

    fun setPoints(list: List<PointF>) {
        points = list
    }

    fun setShape(rect: RectF) {
        shape = rect
    }

    fun setColor(color: Int) {
        paint.color = color
    }

    override fun draw(canvas: Canvas) {
        val path = Path()

        points.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, animator.getAnimatedY(point.y, shape.bottom))
            } else {
                path.lineTo(point.x, animator.getAnimatedY(point.y, shape.bottom))
            }
        }

        canvas.drawPath(path, paint)
    }
}
