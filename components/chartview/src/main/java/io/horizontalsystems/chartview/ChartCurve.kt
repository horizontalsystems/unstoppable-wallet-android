package io.horizontalsystems.chartview

import android.graphics.*
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartPointF

class ChartCurve(private val config: ChartConfig, private val animator: ChartAnimator? = null, override var isVisible: Boolean = false) : ChartDraw {

    private var pointsMap: LinkedHashMap<Long, ChartPointF> = linkedMapOf()
    private var startTimestamp = 0L
    private var endTimestamp = 0L
    private var prevPointsMap: LinkedHashMap<Long, ChartPointF> = linkedMapOf()
    private var prevStartTimestamp = 0L
    private var prevEndTimestamp = 0L
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

    val xxx = Yyy(animator)

    fun setPointsMap(
        pointsMap: LinkedHashMap<Long, ChartPointF>,
        startTimestamp: Long,
        endTimestamp: Long
    ) {
        xxx.setTransitionFrom(
            this.pointsMap,
            this.startTimestamp,
            this.endTimestamp
        )

        xxx.setTransitionTo(
            pointsMap,
            startTimestamp,
            endTimestamp
        )

        xxx.calculate()


        this.prevPointsMap = this.pointsMap
        this.prevStartTimestamp = this.startTimestamp
        this.prevEndTimestamp = this.endTimestamp

        this.pointsMap = pointsMap
        this.startTimestamp = startTimestamp
        this.endTimestamp = endTimestamp

    }

    fun setShape(rect: RectF) {
        shape = rect
    }

    fun setColor(color: Int) {
        paint.color = color
    }

    override fun draw(canvas: Canvas) {
        if (!isVisible) return

//        val xxx = if (animator == null || prevPointsMap.isEmpty()) {
//            pointsMap.values.toList()
//        } else {
//            getPointsForCurrentFrame(pointsMap, prevPointsMap, animator.animatedFraction, startTimestamp, prevStartTimestamp, endTimestamp, prevEndTimestamp)
//        }
//
//        if (xxx.isEmpty()) return

        xxx.nextFrame()
        pointsMap = xxx.getCurrentFramePoints()
        startTimestamp = xxx.getCurrentFrameStartTimestamp()
        endTimestamp = xxx.getCurrentFrameEndTimestamp()

        val xxx = pointsMap.values.toList()

        if (xxx.isEmpty()) return

        val path = Path()

        val startPoint = xxx.first()
        path.moveTo(startPoint.x, startPoint.y)

        for (i in 1 until xxx.size) {
            val point = xxx[i]
            path.lineTo(point.x, point.y)
        }

        canvas.drawPath(path, paint)
    }

    private fun getY(point: PointF) : Float {
        return when {
            animator != null -> animator.getAnimatedY(point.y, shape.bottom)
            else -> point.y
        }
    }
}
