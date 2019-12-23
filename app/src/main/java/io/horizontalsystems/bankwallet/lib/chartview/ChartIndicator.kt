package io.horizontalsystems.bankwallet.lib.chartview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig

class ChartIndicator : View {

    private val touchPoint = TouchPoint(0f, 0f, 0f)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var config: ChartConfig? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun init(config: ChartConfig) {
        this.config = config
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
        val bottom = height - (config?.offsetBottom ?: 0f)
        canvas.drawLine(touchPoint.x, 0f, touchPoint.x, bottom, indicatorPaint)
        canvas.drawRoundRect(touchPoint.x - 15, touchPoint.y - 15, touchPoint.x + 15, touchPoint.y + 15, 20f, 20f, indicatorPaint)
    }

    fun onMove(coordinate: ChartCurve.Coordinate?, listener: ChartView.Listener?) {
        if (coordinate == null || listener == null) return
        if (coordinate.x != touchPoint.last) {
            listener.onTouchSelect(coordinate.point)

            touchPoint.x = coordinate.x
            touchPoint.y = coordinate.y
            touchPoint.last = coordinate.x
            invalidate()
        }
    }

    class TouchPoint(var x: Float, var y: Float, var last: Float)
}
