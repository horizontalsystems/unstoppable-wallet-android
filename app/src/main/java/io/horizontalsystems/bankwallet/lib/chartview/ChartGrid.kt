package io.horizontalsystems.bankwallet.lib.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.GridColumn

class ChartGrid(private val shape: RectF, private val config: ChartConfig) {
    private val gridHelper = GridHelper(shape, config)

    private var gridColumns = listOf<GridColumn>()

    private var gridPaint = Paint()
    private var textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun init(chartType: ChartView.ChartType, startTimestamp: Long, endTimestamp: Long) {
        gridColumns = gridHelper.setGridColumns(chartType, startTimestamp, endTimestamp)

        gridPaint.apply {
            color = config.gridColor
            strokeWidth = config.strokeWidth
        }

        textPaint.apply {
            textSize = config.textSize
            color = config.textColor
            typeface = Typeface.create(config.textFont, Typeface.NORMAL)
        }
    }

    fun draw(canvas: Canvas) {
        if (!config.showGrid) return

        drawColumns(canvas)
        drawFrameLines(canvas)
    }

    private fun drawColumns(canvas: Canvas) {
        gridColumns.forEach {
            if (it.x > config.gridEdgeOffset && shape.right - it.x > config.gridEdgeOffset) {
                canvas.drawLine(it.x, shape.top, it.x, shape.bottom, gridPaint)
            }

            // Labels
            canvas.drawText(it.value, config.xAxisPrice(it.x, shape.right, it.value), shape.bottom + config.textSize + config.textPricePT, textPaint)
        }
    }

    private fun drawFrameLines(canvas: Canvas) {
        // top
        canvas.drawLine(shape.left, shape.top, shape.right, shape.top, gridPaint)
        // left
        canvas.drawLine(shape.left, shape.top, shape.left, shape.bottom, gridPaint)
        // right
        canvas.drawLine(shape.right, shape.top, shape.right, shape.bottom, gridPaint)
        // bottom
        canvas.drawLine(shape.left, shape.bottom, shape.right, shape.bottom, gridPaint)
    }
}
