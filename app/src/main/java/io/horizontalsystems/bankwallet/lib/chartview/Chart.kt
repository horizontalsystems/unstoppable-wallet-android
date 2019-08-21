package io.horizontalsystems.bankwallet.lib.chartview

import android.content.Context
import android.graphics.*
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.horizontalsystems.bankwallet.lib.chartview.models.DataPoint

class Chart(private val context: Context, private val shape: RectF) {

    private val chartHelper = ChartHelper(shape)
    private var points = listOf<DataPoint>()

    private val linePaint = Paint().apply {
        color = context.getColor(R.color.gridStroke)
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = 2f
    }

    private var gradient = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun init(data: ChartData, valueTop: Float, valueStep: Float) {
        points = chartHelper.setPoints(data, valueTop, valueStep)
    }

    fun onTouchActive() {
        setGradient(context.getColor(R.color.chartShaderOverStart), context.getColor(R.color.chartShaderOverEnd))
        linePaint.color = context.getColor(R.color.gridStrokeOver)
    }

    fun onTouchInactive() {
        setGradient(context.getColor(R.color.chartShaderStart), context.getColor(R.color.chartShaderEnd))
        linePaint.color = context.getColor(R.color.gridStroke)
    }

    fun findPoint(value: Float): DataPoint? {
        if (points.size < 2) return null

        val interval = points[1].x - points[0].x
        val lower = value - interval
        val upper = value + interval

        return points.find { it.x > lower && it.x < upper }
    }

    fun draw(canvas: Canvas, animatingFraction: Float) {
        if (points.isEmpty()) {
            return
        }

        canvas.drawChart(animatingFraction)
        canvas.drawGradient(animatingFraction)
    }

    private fun Canvas.drawChart(animatedFraction: Float) {
        val path = Path()

        points.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, animatedYCoordinate(point.y, animatedFraction))
            } else {
                path.lineTo(point.x, animatedYCoordinate(point.y, animatedFraction))
            }
        }

        drawPath(path, linePaint)
    }

    private fun Canvas.drawGradient(animatedFraction: Float) {
        val path = Path()

        if (gradient.shader == null) {
            setGradient(context.getColor(R.color.chartShaderStart), context.getColor(R.color.chartShaderEnd))
        }

        points.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, animatedYCoordinate(point.y, animatedFraction))
            } else {
                path.lineTo(point.x, animatedYCoordinate(point.y, animatedFraction))
            }
        }

        //  Link the last two points
        path.lineTo(points.last().x, shape.bottom)
        path.lineTo(points.first().x, shape.bottom)
        path.close()

        drawPath(path, gradient)
    }

    private fun animatedYCoordinate(y: Float, animatedFraction: Float): Float {
        // Figure out top of column based on INVERSE of percentage. Bigger the percentage,
        // the smaller top is, since 100% goes to 0.
        return shape.bottom - (shape.bottom - y) * animatedFraction
    }

    private fun setGradient(colorStart: Int, colorEnd: Int) {
        gradient.shader = LinearGradient(0f, 0f, 0f, shape.height(), colorStart, colorEnd, Shader.TileMode.CLAMP)
    }
}
