package io.horizontalsystems.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.GridColumn

class ChartGridTimeline(private val config: ChartConfig, override var isVisible: Boolean = true) : ChartDraw {

    private var shape = RectF(0f, 0f, 0f, 0f)
    private var columns = listOf<GridColumn>()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = config.timelineTextSize
        color = config.timelineTextColor
        typeface = Typeface.create(config.textFont, Typeface.NORMAL)
    }

    fun setShape(rect: RectF) {
        shape = rect
    }

    fun setColumns(grids: List<GridColumn>) {
        columns = grids
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColumns()
    }

    private fun Canvas.drawColumns() {
        columns.forEach {
            drawText(it.value, textPosition(it.x, it.value), config.timelineTextSize, textPaint)
        }
    }

    private fun textPosition(x: Float, text: String): Float {
        val width = config.measureTextWidth(text)
        if (width + x >= shape.right) {
            return shape.right - (width + config.timelineTextPadding)
        }

        return x
    }
}
