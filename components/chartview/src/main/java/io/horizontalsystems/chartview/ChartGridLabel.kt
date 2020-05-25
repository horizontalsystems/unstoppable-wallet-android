package io.horizontalsystems.chartview

import android.graphics.*
import io.horizontalsystems.chartview.models.ChartConfig

class ChartGridLabel(private val config: ChartConfig) : ChartDraw {

    private var offset = config.curveVerticalOffset

    private var shape = RectF(0f, 0f, 0f, 0f)
    private var labels = mapOf<String, Paint>()

    fun setShape(rect: RectF) {
        shape = rect
    }

    fun setValues(values: Map<String, Int>) {
        val map = mutableMapOf<String, Paint>()

        values.forEach { (string, labelColor) ->
            map[string] = Paint().apply {
                textSize = config.gridTextSize
                color = labelColor
                typeface = Typeface.create(config.textFont, Typeface.NORMAL)
                isAntiAlias = true
            }
        }

        labels = map
    }

    fun setOffset(padding: Float) {
        offset = padding
    }

    override fun draw(canvas: Canvas) {
        canvas.drawTopLow()
    }

    private fun Canvas.drawTopLow() {
        val bottom = shape.bottom - offset
        var right = shape.right

        labels.forEach { (label, paint) ->
            val textWidth = config.measureTextWidth(label)
            val startX = right - textWidth - config.gridTextPadding

            drawText(label, startX, bottom + config.gridTextSize, paint)

            right = startX
        }
    }
}
