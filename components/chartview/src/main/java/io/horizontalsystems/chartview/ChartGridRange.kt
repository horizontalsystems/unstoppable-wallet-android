package io.horizontalsystems.chartview

import android.graphics.*
import io.horizontalsystems.chartview.models.ChartConfig

class ChartGridRange(private val config: ChartConfig, override var isVisible: Boolean = true) : ChartDraw {

    private var top: String = ""
    private var low: String = ""
    private var offset = config.curveVerticalOffset

    private var shape = RectF(0f, 0f, 0f, 0f)

    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = config.gridDashColor
        strokeWidth = config.strokeDashWidth
        pathEffect = DashPathEffect(floatArrayOf(config.strokeDash, config.strokeDash), 0f)
    }

    private var textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = config.gridTextSize
        color = config.gridTextColor
        typeface = config.textFont
    }

    fun setShape(rect: RectF) {
        shape = rect
    }

    fun setValues(top: String, low: String) {
        this.top = top
        this.low = low
    }

    fun setOffset(padding: Float) {
        offset = padding
    }

    override fun draw(canvas: Canvas) {
        if (!isVisible) return
        canvas.drawTopLow()
    }

    private fun Canvas.drawTopLow() {
        val path = Path()

        val topY = shape.top + offset
        val bottomY = shape.bottom - offset

        path.moveTo(shape.left, topY)
        path.lineTo(shape.right, topY)
        drawPath(path, dashPaint)

        path.moveTo(shape.left, bottomY)
        path.lineTo(shape.right, bottomY)
        drawPath(path, dashPaint)

        // Texts
        drawText(top, shape.left + config.gridTextPadding, textPosition(topY, isTop = true), textPaint)
        drawText(low, shape.left + config.gridTextPadding, textPosition(bottomY, isTop = false), textPaint)
    }

    private fun textPosition(y: Float, isTop: Boolean): Float {
        if (isTop) {
            return y - config.gridTextPadding
        }

        return y + config.gridTextSize
    }
}
