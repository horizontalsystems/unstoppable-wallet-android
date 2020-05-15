package io.horizontalsystems.chartview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import io.horizontalsystems.chartview.models.ChartConfig

class ChartTouchArea @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    private var listener: Chart.Listener? = null

    private var touchPoint: TouchPoint? = null
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var offsetBottom = 0f
    private var coordinates = listOf<Coordinate>()

    fun configure(config: ChartConfig, bottomOffset: Float) {
        offsetBottom = bottomOffset

        linePaint.apply {
            color = config.cursorColor
            style = Paint.Style.FILL
            strokeWidth = config.strokeWidth
        }
    }

    fun set(points: List<Coordinate>) {
        coordinates = points
    }

    fun onUpdate(eventListener: Chart.Listener) {
        listener = eventListener
    }

    override fun willNotDraw(): Boolean {
        return false
    }

    override fun onDraw(canvas: Canvas) {
        val touch = touchPoint ?: return
        val bottom = height - offsetBottom

        canvas.drawLine(touch.x, 0f, touch.x, bottom, linePaint)
        canvas.drawRoundRect(touch.x - 15, touch.y - 15, touch.x + 15, touch.y + 15, 20f, 20f, linePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val listener = listener ?: return false

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
        if (coordinate.x != touchPoint?.last) {
            listener.onTouchSelect(coordinate.point)

            touchPoint = TouchPoint(coordinate.x, coordinate.y, coordinate.x)
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

    class TouchPoint(var x: Float, var y: Float, var last: Float)
}
