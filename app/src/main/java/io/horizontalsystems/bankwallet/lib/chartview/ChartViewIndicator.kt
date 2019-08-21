package io.horizontalsystems.bankwallet.lib.chartview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.lib.chartview.models.DataPoint

class ChartViewIndicator : View {

    private val touchPoint = TouchPoint(0f, 0f)
    private var lastPointX = 0f

    private val indicatorPaint = Paint().apply {
        color = context.getColor(R.color.indicator)
        style = Paint.Style.FILL
        strokeWidth = 2f
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun willNotDraw(): Boolean {
        return false
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawLine(touchPoint.x, 0f, touchPoint.x, height.toFloat(), indicatorPaint)
        canvas.drawRoundRect(touchPoint.x - 11, touchPoint.y - 11, touchPoint.x + 11, touchPoint.y + 11, 20f, 20f, indicatorPaint)
    }

    fun onMove(point: DataPoint?, listener: ChartView.Listener?) {
        if (point == null || listener == null) return
        if (point.x != lastPointX) {
            listener.onTouchMove(point)

            touchPoint.x = point.x
            touchPoint.y = point.y
            invalidate()
        }
    }

    class TouchPoint(var x: Float, var y: Float)
}
