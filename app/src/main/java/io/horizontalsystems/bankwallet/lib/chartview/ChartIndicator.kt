package io.horizontalsystems.bankwallet.lib.chartview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.DataPoint

class ChartIndicator : View {

    private val touchPoint = TouchPoint(0f, 0f, 0f)
    private val indicatorPaint = Paint()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun init(config: ChartConfig) {
        indicatorPaint.apply {
            color = config.indicatorColor
            style = Paint.Style.FILL
            strokeWidth = config.strokeWidth
        }
    }

    override fun willNotDraw(): Boolean {
        return false
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawLine(touchPoint.x, 0f, touchPoint.x, height.toFloat(), indicatorPaint)
        canvas.drawRoundRect(touchPoint.x - 15, touchPoint.y - 15, touchPoint.x + 15, touchPoint.y + 15, 20f, 20f, indicatorPaint)
    }

    fun onMove(point: DataPoint?, listener: ChartView.Listener?) {
        if (point == null || listener == null) return
        if (point.x != touchPoint.last) {
            listener.onTouchMove(point)

            touchPoint.x = point.x
            touchPoint.y = point.y
            touchPoint.last = point.x
            invalidate()
        }
    }

    class TouchPoint(var x: Float, var y: Float, var last: Float)
}
