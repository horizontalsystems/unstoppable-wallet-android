package io.horizontalsystems.chartview

import android.graphics.*
import io.horizontalsystems.chartview.models.ChartConfig

class ChartGridDash(private val config: ChartConfig) : ChartDraw {

    private var top: String = ""
    private var low: String = ""
    private var offset = config.curveVerticalOffset

    private var shape = RectF(0f, 0f, 0f, 0f)

    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = config.gridDottedColor
        strokeWidth = config.strokeWidthDotted
        pathEffect = DashPathEffect(floatArrayOf(config.strokeDotted, config.strokeDotted), 0f)
    }

    private var textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = config.textPriceSize
        color = config.textPriceColor
        typeface = Typeface.create(config.textFont, Typeface.BOLD)
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
        canvas.drawTopLow()
    }

    private fun Canvas.drawTopLow() {
        val path = Path()

        val topY = offset
        val bottomY = shape.bottom - offset

        path.moveTo(shape.left, topY)
        path.lineTo(shape.right, topY)
        drawPath(path, dashPaint)

        path.moveTo(shape.left, bottomY)
        path.lineTo(shape.right, bottomY)
        drawPath(path, dashPaint)

        // Texts
        drawText(top, shape.left + config.textPricePL, config.yAxisPrice(topY, isTop = true), textPaint)
        drawText(low, shape.left + config.textPricePL, config.yAxisPrice(bottomY, isTop = false), textPaint)
    }
}
