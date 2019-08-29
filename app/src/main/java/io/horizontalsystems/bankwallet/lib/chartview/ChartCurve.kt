package io.horizontalsystems.bankwallet.lib.chartview

import android.graphics.*
import androidx.core.graphics.ColorUtils.setAlphaComponent
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.horizontalsystems.bankwallet.lib.chartview.models.DataPoint

class ChartCurve(private val shape: RectF, private val config: ChartConfig) {

    private val chartHelper = ChartHelper(shape, config)
    private var points = listOf<DataPoint>()

    private val linePaint = Paint()
    private var gradient = Paint()

    fun init(data: ChartData) {
        points = chartHelper.setPoints(data)
        onTouchInactive()

        linePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = config.strokeWidth
            isAntiAlias = true
        }

        gradient.apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    fun onTouchActive() {
        setGradient(setAlphaComponent(config.touchColor, 104), setAlphaComponent(config.touchColor, 13))
        linePaint.color = config.touchColor
    }

    fun onTouchInactive() {
        setGradient(setAlphaComponent(config.curveColor, 104), setAlphaComponent(config.curveColor, 13))
        linePaint.color = config.curveColor
    }

    fun findPoint(value: Float): DataPoint? {
        if (points.size < 2) return null

        val interval = points[1].x - points[0].x
        val lower = value - interval
        val upper = value + interval

        return points.find { it.x > lower && it.x < upper }
    }

    fun draw(canvas: Canvas) {
        if (points.isEmpty()) {
            return
        }

        canvas.drawChart()
        canvas.drawGradient()
    }

    private fun Canvas.drawChart() {
        val path = Path()

        points.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, animatedY(point.y))
            } else {
                path.lineTo(point.x, animatedY(point.y))
            }
        }

        drawPath(path, linePaint)
    }

    private fun Canvas.drawGradient() {
        val path = Path()

        points.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, animatedY(point.y))
            } else {
                path.lineTo(point.x, animatedY(point.y))
            }
        }

        //  Link the last two points
        path.lineTo(points.last().x, shape.bottom)
        path.lineTo(points.first().x, shape.bottom)
        path.close()

        drawPath(path, gradient)
    }

    private fun animatedY(y: Float): Float {
        if (!config.animated) return y

        // Figure out top of column based on INVERSE of percentage. Bigger the percentage,
        // the smaller top is, since 100% goes to 0.
        return shape.bottom - (shape.bottom - y) * config.animatedFraction
    }

    private fun setGradient(colorStart: Int, colorEnd: Int) {
        gradient.shader = LinearGradient(0f, 0f, 0f, shape.bottom, colorStart, colorEnd, Shader.TileMode.CLAMP)
    }
}
