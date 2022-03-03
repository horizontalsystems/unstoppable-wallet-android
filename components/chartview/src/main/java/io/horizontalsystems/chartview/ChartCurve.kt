package io.horizontalsystems.chartview

import android.graphics.*
import android.util.Log
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartPointF

class ChartCurve(private val config: ChartConfig, private val animator: ChartAnimator? = null, override var isVisible: Boolean = false) : ChartDraw {

    private var pointsMap: LinkedHashMap<Long, ChartPointF> = linkedMapOf()
    private var startTimestamp = 0L
    private var endTimestamp = 0L

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

    var xxx: Yyy? = null

    fun setPointsMap(
        pointsMap: LinkedHashMap<Long, ChartPointF>,
        startTimestamp: Long,
        endTimestamp: Long
    ) {
        Log.e("AAA", "setPointsMap animatedFraction: ${animator?.animatedFraction}")
        xxx = Yyy(animator,
            this.pointsMap,
            this.startTimestamp,
            this.endTimestamp,
            pointsMap,
            startTimestamp,
            endTimestamp
        )
    }

    fun setShape(rect: RectF) {
        shape = rect
    }

    fun setColor(color: Int) {
        paint.color = color
    }

    override fun draw(canvas: Canvas) {
        if (!isVisible) return

        xxx?.let { xxx ->
            xxx.nextFrame()
            val currentFramePoints = xxx.getCurrentFramePoints()

            val currentPoints = currentFramePoints.values.toList()

            if (currentPoints.isNotEmpty()) {
//                val xs = currentPoints.map { it.x }
//                if (xs != xs.sorted()) {
//                    Log.e("AAA", "Yaaaaa")
//                    Log.e("AAA", "\n${xs.joinToString("\n")}\n")
//                }

                val path = Path()

                val startPoint = currentPoints.first()
                path.moveTo(startPoint.x, startPoint.y)

                for (i in 1 until currentPoints.size) {
                    val point = currentPoints[i]
                    path.lineTo(point.x, point.y)
                }

                canvas.drawPath(path, paint)
            }


            pointsMap = currentFramePoints
            startTimestamp = xxx.getCurrentFrameStartTimestamp()
            endTimestamp = xxx.getCurrentFrameEndTimestamp()
        }


    }

    private fun getY(point: PointF) : Float {
        return when {
            animator != null -> animator.getAnimatedY(point.y, shape.bottom)
            else -> point.y
        }
    }
}
