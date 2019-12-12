package io.horizontalsystems.bankwallet.lib.chartview

import android.graphics.*
import androidx.core.graphics.ColorUtils.setAlphaComponent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint

class ChartCurve(private val shape: RectF, private val config: ChartConfig) {

    private val chartHelper = ChartHelper(shape, config)
    private var coordinates = listOf<Coordinate>()
    private var coordinateTop: Coordinate? = null
    private var coordinateLow: Coordinate? = null

    private val linePaint = Paint()
    private val gridPaint = Paint()
    private val gradient = Paint()
    private var textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isTouchActive = false

    private var currency: Currency? = null

    fun init(points: List<ChartPoint>, startTimestamp: Long, endTimestamp: Long, baseCurrency: Currency?) {
        currency = baseCurrency
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
            drawText(format(top.point.value), shape.left + config.textPricePadding, config.yAxisPrice(top.y, isTop = true), textPaint)
            drawText(format(low.point.value), shape.left + config.textPricePadding, config.yAxisPrice(low.y, isTop = false), textPaint)
        }
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

    private fun format(value: Float): String {
        val baseCurrency = currency ?: return ""
        val currencyValue = CurrencyValue(baseCurrency, value.toBigDecimal())
        return App.numberFormatter.format(currencyValue, canUseLessSymbol = false, maxFraction = 8) ?: ""
    }

    class Coordinate(val x: Float, val y: Float, val point: ChartPoint)
}
