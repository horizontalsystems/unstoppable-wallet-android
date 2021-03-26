package io.horizontalsystems.chartview

import android.graphics.*
import io.horizontalsystems.chartview.models.ChartConfig

class ChartBottomLabel(private val config: ChartConfig, override var isVisible: Boolean = false) : ChartDraw {

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
                typeface = config.textFont
                isAntiAlias = true
            }
        }

        labels = map
    }

    fun setOffset(padding: Float) {
        offset = padding
    }

    override fun draw(canvas: Canvas) {
        if (!isVisible) return
        canvas.drawLabels()
    }

    private fun Canvas.drawLabels() {
        val bottom = shape.bottom - offset
        var right = shape.right - config.gridSideTextPadding

        labels.forEach { (label, paint) ->
            val textWidth = paint.measureText(label)
            val startX = right - textWidth - config.gridTextPadding

            drawText(label, startX, bottom + config.gridTextSize, paint)

            right = startX
        }
    }
}
