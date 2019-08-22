package io.horizontalsystems.bankwallet.lib.chartview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.horizontalsystems.bankwallet.lib.chartview.models.GridColumn
import io.horizontalsystems.bankwallet.lib.chartview.models.GridLine

class ChartGrid(private val context: Context, private val shape: RectF) {
    private val gridHelper = GridHelper(shape)

    private var offsetWidth = 0f
    private var gridColumns = listOf<GridColumn>()
    private var gridLines = listOf<GridLine>()

    private var gridPaint = Paint().apply {
        color = context.getColor(R.color.grid)
        strokeWidth = 2f
    }

    private var textPaint = Paint().apply {
        textSize = 24f
        color = context.getColor(R.color.gridText)
        style = Paint.Style.FILL
    }

    fun init(data: ChartData, valueTop: Float, valueStep: Float, valueWidth: Float) {
        offsetWidth = valueWidth
        gridColumns = gridHelper.setGridColumns(data)
        gridLines = gridHelper.setGridLines(valueTop, valueStep)
    }

    fun draw(canvas: Canvas) {
        drawLines(canvas)
        drawColumns(canvas)
        drawFrameLines(canvas)
    }

    private fun drawLines(canvas: Canvas) {
        gridLines.forEach {
            canvas.drawLine(shape.left, it.y, shape.right, it.y, gridPaint)
            canvas.drawLine(shape.right, it.y, shape.right + offsetWidth, it.y, gridPaint)

            // Labels
            canvas.drawText(it.value, shape.right + 8, it.y + 30, textPaint)
        }
    }

    private fun drawColumns(canvas: Canvas) {
        gridColumns.forEach {
            if (shape.right - it.x > 5f) {
                canvas.drawLine(it.x, shape.top, it.x, shape.bottom, gridPaint)
            }

            // Labels
            canvas.drawText(it.value, it.x, shape.bottom + 25, textPaint)
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
        canvas.drawLine(shape.left, shape.bottom, shape.right + offsetWidth, shape.bottom, gridPaint)
    }
}
