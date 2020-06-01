package io.horizontalsystems.chartview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import io.horizontalsystems.chartview.models.ChartConfig

class ChartTouchArea @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    private var listener: Chart.Listener? = null

    private var touchPoint: PointF? = null
    private var offsetBottom = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var coordinates = listOf<Coordinate>()

    fun configure(config: ChartConfig, bottomOffset: Float) {
        offsetBottom = bottomOffset

        paint.color = config.cursorColor
        paint.style = Paint.Style.FILL
        paint.strokeWidth = config.strokeWidth
    }

    fun setCoordinates(list: List<Coordinate>) {
        coordinates = list
    }

    fun onUpdate(eventListener: Chart.Listener) {
        listener = eventListener
    }

    override fun willNotDraw(): Boolean {
        return false
    }

    override fun onDraw(canvas: Canvas) {
        val point = touchPoint ?: return

        canvas.drawLine(point.x, 0f, point.x, height - offsetBottom, paint)
        canvas.drawRoundRect(point.x - 15, point.y - 15, point.x + 15, point.y + 15, 20f, 20f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val listener = listener ?: return false
        if (coordinates.isEmpty()) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                listener.onTouchDown()
                onMove(findNearest(event.x), listener)
            }

            MotionEvent.ACTION_MOVE -> {
                onMove(findNearest(event.x), listener)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                touchPoint = null
                invalidate()
                listener.onTouchUp()
            }
        }

        return true
    }

    private fun onMove(coordinate: Coordinate?, listener: Chart.Listener) {
        if (coordinate == null) return
        if (coordinate.x != touchPoint?.x) {
            listener.onTouchSelect(coordinate.point)

            touchPoint = PointF(coordinate.x, coordinate.y)
            invalidate()
        }
    }

    private fun findNearest(point: Float): Coordinate? {
        for ((index, currPoint) in coordinates.withIndex()) {
            if (currPoint.x == point) {
                return currPoint
            }

            if (point < currPoint.x) {
                if (index - 1 < 0) {
                    return currPoint
                }

                val prevPoint = coordinates[index - 1]
                val halfInterval = (currPoint.x - prevPoint.x) / 2
                val nearPrevious = (prevPoint.x + halfInterval) > point
                if (nearPrevious) {
                    return prevPoint
                } else {
                    return currPoint
                }
            }

            if (index + 1 > coordinates.size) {
                return currPoint
            }
        }

        return null
    }
}
