package io.horizontalsystems.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartPointF

class ChartCurveXxx(
    private val config: ChartConfig,
    private val animator: ChartAnimator? = null
) : ChartDraw {
    override var isVisible: Boolean = true

    private var shape = RectF(0f, 0f, 0f, 0f)

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = config.curveColor
        strokeWidth = config.strokeWidth
        isAntiAlias = true
    }

    private var curveVerticalOffset = 0f
    fun setCurveVerticalOffset(v: Float) {
        curveVerticalOffset = v
    }
    fun setShape(rect: RectF) {
        shape = rect
    }

    fun setColor(color: Int) {
        paint.color = color
    }

    private var pointsManager: PointsManager? = null
    var zzz: Zzz? = null

    fun setChartData(data: ChartData) {
        zzz = Zzz(
            data,
            zzz?.frameValues ?: linkedMapOf(),
            zzz?.frameStartTimestamp ?: 0L,
            zzz?.frameEndTimestamp ?: 0L,
            zzz?.frameMinValue ?: 0f,
            zzz?.frameMaxValue ?: 0f,
        )
        pointsManager = PointsManager(
            shape.right,
            shape.bottom,
            curveVerticalOffset
        )
    }

    override fun draw(canvas: Canvas) {
        if (!isVisible) return
//        if (valuesByTimestamp.isEmpty()) return

        val tmpZzz = zzz ?: return
        val tmpYyy = pointsManager ?: return

        tmpZzz.nextFrame(animator?.animatedFraction ?: 1f)

        val points = tmpYyy.getPoints(
            tmpZzz.frameValues,
            tmpZzz.frameStartTimestamp,
            tmpZzz.frameEndTimestamp,
            tmpZzz.frameMinValue,
            tmpZzz.frameMaxValue,
        )

        if (points.isEmpty()) return

        val path = Path()
        val firstPoint = points.first()
        path.moveTo(firstPoint.x, firstPoint.y)

        for (i in 1 until points.size) {
            val chartPointF = points[i]
            path.lineTo(chartPointF.x, chartPointF.y)
        }

        canvas.drawPath(path, paint)
    }

    private fun getY(point: ChartPointF): Float {
        return when {
            animator != null -> animator.getAnimatedY(point.y, shape.bottom)
            else -> point.y
        }
    }

}
