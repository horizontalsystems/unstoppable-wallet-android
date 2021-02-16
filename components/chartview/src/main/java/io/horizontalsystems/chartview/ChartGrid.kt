package io.horizontalsystems.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.GridColumn

class ChartGrid(private val config: ChartConfig, override var isVisible: Boolean = true) : ChartDraw {

    private var shape = RectF(0f, 0f, 0f, 0f)
    private var columns = listOf<GridColumn>()

    private val linePaint = Paint().apply {
        color = config.gridLineColor
        strokeWidth = config.strokeWidth
    }

    fun setShape(rect: RectF) {
        shape = rect
    }

    fun set(grids: List<GridColumn>) {
        columns = grids
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColumns()
        canvas.drawFrameLines()
    }

    private fun Canvas.drawColumns() {
        columns.forEach {
            if (it.x > config.gridEdgeOffset && shape.right - it.x > config.gridEdgeOffset) {
                drawLine(it.x, shape.top, it.x, shape.bottom, linePaint)
            }
        }
    }

    private fun Canvas.drawFrameLines() {
        // top
        drawLine(shape.left, shape.top, shape.right, shape.top, linePaint)
        // left
        // drawLine(shape.left, shape.top, shape.left, shape.bottom, linePaint)
        // right
        // drawLine(shape.right, shape.top, shape.right, shape.bottom, linePaint)
        // bottom
        drawLine(shape.left, shape.bottom, shape.right, shape.bottom, linePaint)
    }
}
