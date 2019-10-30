package io.horizontalsystems.bankwallet.lib.chartview

import android.graphics.*
import androidx.core.graphics.ColorUtils.setAlphaComponent
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint

class ChartCurve(private val shape: RectF, private val config: ChartConfig) {

    private val chartHelper = ChartHelper(shape, config)
    private var coordinates = listOf<Coordinate>()

    private val linePaint = Paint()
    private val gradient = Paint()

    fun init(points: List<ChartPoint>, startTimestamp: Long, endTimestamp: Long) {
        coordinates = chartHelper.setCoordinates(points, startTimestamp, endTimestamp)

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
        setGradient(setAlphaComponent(config.touchColor, 0xCC), setAlphaComponent(config.touchColor, 0x0D))
        linePaint.color = config.touchColor
    }

    fun onTouchInactive() {
        setGradient(setAlphaComponent(config.curveColor, 0xCC), setAlphaComponent(config.curveColor, 0x0D))
        linePaint.color = config.curveColor
    }

    fun find(value: Float): Coordinate? {
        if (coordinates.size < 2) return null
        if (coordinates.last().x <= value) {
            return coordinates.last()
        }

        val interval = coordinates[1].x - coordinates[0].x
        val lower = value - interval
        val upper = value + interval

        return coordinates.find { it.x > lower && it.x < upper }
    }

    fun draw(canvas: Canvas) {
        if (coordinates.isEmpty()) {
            return
        }

        canvas.drawChart()
        canvas.drawGradient()
    }

    private fun Canvas.drawChart() {
        val path = Path()

        coordinates.forEachIndexed { index, point ->
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

        coordinates.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, animatedY(point.y))
            } else {
                path.lineTo(point.x, animatedY(point.y))
            }
        }

        //  Link the last two points
        path.lineTo(coordinates.last().x, shape.bottom)
        path.lineTo(coordinates.first().x, shape.bottom)
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
        gradient.shader = LinearGradient(0f, 0f, 0f, shape.bottom + 2, colorStart, colorEnd, Shader.TileMode.REPEAT)
    }

    class Coordinate(val x: Float, val y: Float, val point: ChartPoint)
}
