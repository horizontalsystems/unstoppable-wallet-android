package io.horizontalsystems.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import io.horizontalsystems.chartview.models.ChartConfig

class ChartCurve2(private val config: ChartConfig) : ChartDraw {
    override var isVisible: Boolean = true

    private var shape = RectF(0f, 0f, 0f, 0f)

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = config.curveColor
        strokeWidth = config.strokeWidth
        isAntiAlias = true
    }

    private var curveAnimator: CurveAnimator? = null

    fun setShape(rect: RectF) {
        shape = rect
    }

    fun setColor(color: Int) {
        paint.color = color
    }

    fun setCurveAnimator(curveAnimator: CurveAnimator) {
        this.curveAnimator = curveAnimator
    }

    override fun draw(canvas: Canvas) {
        if (!isVisible) return
        val tmpCurveAnimator = curveAnimator ?: return

        val points = tmpCurveAnimator.getFramePoints(paint.strokeWidth / 2f)
        if (points.isEmpty()) return
        if (points.size == 1) {
            val pointF = points.first()
            canvas.drawLine(0f, pointF.y, shape.width(), pointF.y, paint)
            return
        }

        val path = Path()
        val firstPoint = points.first()
        path.moveTo(firstPoint.x, firstPoint.y)

        for (i in 1 until points.size) {
            val chartPointF = points[i]
            path.lineTo(chartPointF.x, chartPointF.y)
        }

        canvas.drawPath(path, paint)
    }

}
