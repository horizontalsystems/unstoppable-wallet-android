package io.horizontalsystems.chartview

import android.graphics.*
import androidx.core.graphics.ColorUtils.setAlphaComponent
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartPoint

class ChartCurve(private val shape: RectF, private val config: ChartConfig) {

    var formatter: ChartView.RateFormatter? = null

    private val chartHelper = ChartHelper(shape, config)
    private var coordinates = listOf<Coordinate>()
    private var coordinateTop: Coordinate? = null
    private var coordinateLow: Coordinate? = null

    private val linePaint = Paint()
    private val gridPaint = Paint()
    private val gradient = Paint()
    private var textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isTouchActive = false

    fun init(points: List<ChartPoint>, startTimestamp: Long, endTimestamp: Long) {
        coordinates = chartHelper.setCoordinates(points, startTimestamp, endTimestamp)

        val (top, low) = chartHelper.getTopAndLow(coordinates)
        coordinateTop = top
        coordinateLow = low

        onTouchInactive()

        linePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = config.strokeWidth
            isAntiAlias = true
        }

        val dottedWidth = config.dp2px(2f)
        gridPaint.apply {
            color = config.gridDottedColor
            strokeWidth = config.strokeWidthDotted
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(dottedWidth, dottedWidth), 0f)
        }

        textPaint.apply {
            textSize = config.textPriceSize
            color = config.textPriceColor
            typeface = Typeface.create(config.textFont, Typeface.BOLD)
        }

        gradient.apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    fun onTouchActive() {
        isTouchActive = true
        setGradient(setAlphaComponent(config.touchColor, 0xCC), setAlphaComponent(config.touchColor, 0x0D))
        linePaint.color = config.touchColor
    }

    fun onTouchInactive() {
        isTouchActive = false
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
        canvas.drawTopLow()
    }

    private fun Canvas.drawChart() {
        val path = Path()

        coordinates.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, config.getAnimatedY(point.y, shape.bottom))
            } else {
                path.lineTo(point.x, config.getAnimatedY(point.y, shape.bottom))
            }
        }

        drawPath(path, linePaint)
    }

    private fun Canvas.drawGradient() {
        val path = Path()

        coordinates.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, config.getAnimatedY(point.y, shape.bottom))
            } else {
                path.lineTo(point.x, config.getAnimatedY(point.y, shape.bottom))
            }
        }

        //  Link the last two points
        path.lineTo(coordinates.last().x, shape.bottom)
        path.lineTo(coordinates.first().x, shape.bottom)
        path.close()

        drawPath(path, gradient)
    }

    private fun Canvas.drawTopLow() {
        val top = coordinateTop
        val low = coordinateLow
        if (top == null || low == null || !config.showGrid) {
            return
        }

        val path = Path()

        path.moveTo(shape.left, top.y)
        path.lineTo(shape.right, top.y)
        drawPath(path, gridPaint)

        path.moveTo(shape.left, low.y)
        path.lineTo(shape.right, low.y)
        drawPath(path, gridPaint)

        if (!isTouchActive) {
            drawText(format(top.point.value, getMaxFraction(top.point.value)), shape.left + config.textPricePL, config.yAxisPrice(top.y, isTop = true), textPaint)
            drawText(format(low.point.value, getMaxFraction(top.point.value)), shape.left + config.textPricePL, config.yAxisPrice(low.y, isTop = false), textPaint)
        }
    }

    private fun getMaxFraction(value: Float): Int?{
        val fiatBigNumber = 1000f
        return if (config.valueScale == 0 || value > fiatBigNumber) null else config.valueScale
    }

    private fun setGradient(colorStart: Int, colorEnd: Int) {
        gradient.shader = LinearGradient(0f, 0f, 0f, shape.bottom + 2, colorStart, colorEnd, Shader.TileMode.REPEAT)
    }

    private fun format(value: Float, maxFraction: Int?): String {
        return formatter?.format(value.toBigDecimal(), maxFraction) ?: ""
    }

    class Coordinate(val x: Float, val y: Float, val point: ChartPoint)
}
