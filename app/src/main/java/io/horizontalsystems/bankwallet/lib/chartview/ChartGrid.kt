package io.horizontalsystems.bankwallet.lib.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.GridColumn
import io.horizontalsystems.bankwallet.lib.chartview.models.GridLine

class ChartGrid(private val shape: RectF, private val config: ChartConfig) {
    private val gridHelper = GridHelper(shape, config)

    private var gridColumns = listOf<GridColumn>()
    private var gridLines = listOf<GridLine>()

    private var gridPaint = Paint()
    private var textPaint = Paint()

    fun init(chartType: ChartView.ChartType, startTimestamp: Long, endTimestamp: Long) {
        gridColumns = gridHelper.setGridColumns(chartType, startTimestamp, endTimestamp)
        gridLines = gridHelper.setGridLines()

        gridPaint.apply {
            color = config.gridColor
            strokeWidth = config.strokeWidth
        }

        textPaint.apply {
            textSize = config.textSize
            style = Paint.Style.FILL
            color = config.textColor
        }
    }

    fun draw(canvas: Canvas) {
        if (!config.showGrid) return

        drawLines(canvas)
        drawColumns(canvas)
        drawFrameLines(canvas)
    }

    private fun drawLines(canvas: Canvas) {
        gridLines.forEach {
            canvas.drawLine(shape.left, it.y, shape.right, it.y, gridPaint)
            canvas.drawLine(shape.right, it.y, shape.right + config.offsetRight, it.y, gridPaint)

            // Labels
            canvas.drawText(it.value, shape.right + config.textPadding, it.y + config.textSize + config.textPadding, textPaint)
        }
    }

    private fun drawColumns(canvas: Canvas) {
        gridColumns.forEach {
            if (it.x > config.gridEdgeOffset && shape.right - it.x > config.gridEdgeOffset) {
                canvas.drawLine(it.x, shape.top, it.x, shape.bottom, gridPaint)
            }

            // Labels
            canvas.drawText(it.value, it.x, shape.bottom + config.textSize + config.textPadding, textPaint)
        }
    }

    private fun drawFrameLines(canvas: Canvas) {
        // left
        canvas.drawLine(shape.left, shape.top, shape.left, shape.bottom, gridPaint)
        // right
        canvas.drawLine(shape.right, shape.top, shape.right, shape.bottom, gridPaint)
        // right of price columns
        // canvas.drawLine(shape.right + offsetWidth, shape.top, shape.right + offsetWidth, shape.bottom, gridPaint)
        // bottom
        canvas.drawLine(shape.left, shape.bottom, shape.right + config.offsetRight, shape.bottom, gridPaint)
    }
}
