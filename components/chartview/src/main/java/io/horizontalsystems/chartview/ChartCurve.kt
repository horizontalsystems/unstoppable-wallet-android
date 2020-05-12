package io.horizontalsystems.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartConfig

class ChartCurve(private val config: ChartConfig, private val animator: ChartAnimator) : ChartDraw {

    private var shape = RectF(0f, 0f, 0f, 0f)
    private var points = listOf<Coordinate>()

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = config.curveColor
        strokeWidth = config.strokeWidth
        isAntiAlias = true
    }

    fun setPoints(coordinates: List<Coordinate>) {
        points = coordinates
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
