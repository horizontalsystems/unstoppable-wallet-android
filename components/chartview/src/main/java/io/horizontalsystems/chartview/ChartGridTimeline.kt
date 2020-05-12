package io.horizontalsystems.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.GridColumn

class ChartGridTimeline(private val config: ChartConfig) : ChartDraw {

    private var shape = RectF(0f, 0f, 0f, 0f)
    private var columns = listOf<GridColumn>()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = config.timelineTextSize
        color = config.textColor
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
            drawText(it.value, config.xAxisPrice(it.x, shape.right, it.value), config.timelineTextPadding, textPaint)
        }
    }
}
