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

    fun set(points: List<Coordinate>, config: ChartConfig) {
        coordinates = points
        offsetBottom = config.curveVerticalOffset

        linePaint.apply {
            color = config.cursorColor
            style = Paint.Style.FILL
            strokeWidth = config.strokeWidth
        }
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

        //  vertical line  draw
        canvas.drawLine(touch.x, 0f, touch.x, bottom, linePaint)
        canvas.drawRoundRect(touch.x - 15, touch.y - 15, touch.x + 15, touch.y + 15, 20f, 20f, linePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val listener = listener ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                listener.onTouchDown()
                onMove(find(event.x), listener)
            }

            MotionEvent.ACTION_MOVE -> {
                onMove(find(event.x), listener)
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

    private fun find(value: Float): Coordinate? {
        if (coordinates.size < 2) return null
        if (coordinates.last().x <= value) {
            return coordinates.last()
        }

        val interval = coordinates[1].x - coordinates[0].x
        val lower = value - interval
        val upper = value + interval

        return coordinates.find { it.x > lower && it.x < upper }
    }

    class TouchPoint(var x: Float, var y: Float, var last: Float)
}
