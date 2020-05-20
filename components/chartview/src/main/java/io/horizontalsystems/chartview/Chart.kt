package io.horizontalsystems.chartview

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.chartview.helpers.*
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.views.showIf
import kotlinx.android.synthetic.main.view_chart.view.*
import java.math.BigDecimal

interface ChartDraw {
    fun draw(canvas: Canvas)
}

class Chart @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onTouchDown()
        fun onTouchUp()
        fun onTouchSelect(point: ChartPoint)
    }

    interface RateFormatter {
        fun format(value: BigDecimal): String?
    }

    init {
        inflate(context, R.layout.view_chart, this)
    }

    var rateFormatter: RateFormatter? = null

    private val config = ChartConfig(context, attrs)
    private val animator = ChartAnimator {
        chartMain.invalidate()
        chartBottom.invalidate()
        chartTimeline.invalidate()
    }

    private val mainCurve = ChartCurve(config, animator)
    private val mainGradient = ChartGradient(animator)
    private val bottomVolume = ChartVolume(config, animator)
    private val gridMain = ChartGrid(config)
    private val gridBottom = ChartGrid(config)
    private val gridDashMain = ChartGridDash(config)
    private val gridDashBottom = ChartGridDash(config)
    private val gridTimeline = ChartGridTimeline(config)

    private val emaFastCurve = ChartCurve(config, animator)
    private val emaSlowCurve = ChartCurve(config, animator)

    private val macdCurve = ChartCurve(config, animator)
    private val macdSignalCurve = ChartCurve(config, animator)
    private val macdHistogram = ChartHistogram(config, animator)

    private val rsiCurve = ChartCurve(config, animator)

    fun setListener(listener: Listener) {
        chartTouch.onUpdate(object : Listener {
            override fun onTouchDown() {
                mainCurve.setColor(config.curvePressedColor)
                mainGradient.setShader(config.curvePressedColor)
                chartMain.invalidate()
                listener.onTouchDown()
            }

            override fun onTouchUp() {
                mainCurve.setColor(config.curveColor)
                mainGradient.setShader(config.curveColor)
                chartMain.invalidate()
                listener.onTouchUp()
            }

            override fun onTouchSelect(point: ChartPoint) {
                listener.onTouchSelect(point)
            }
        })
    }

    fun showSinner() {
        chartError.showIf(false)
        chartViewSpinner.showIf(true)
        loadingShade.showIf(true)
    }

    fun hideSinner() {
        chartViewSpinner.showIf(false)
        loadingShade.showIf(false)
    }

    fun showError(error: String) {
        chartMain.visibility = View.INVISIBLE
        chartError.visibility = View.VISIBLE
        chartError.text = error
    }

    fun setData(data: ChartData, chartType: ChartView.ChartType) {
        config.setTrendColor(data)

        val shapeMain = chartMain.shape
        val shapeBottom = chartBottom.shape
        val shapeTimeline = chartTimeline.shape

        val emaFast = PointConverter.curve(data.values(Indicator.EmaFast), shapeMain, config.curveVerticalOffset)
        val emaSlow = PointConverter.curve(data.values(Indicator.EmaSlow), shapeMain, config.curveVerticalOffset)
        val rsi = PointConverter.curve(data.values(Indicator.Rsi), shapeBottom, 0f)

        val macd = PointConverter.curve(data.values(Indicator.Macd), shapeBottom, config.macdLineOffset)
        val macdSignal = PointConverter.curve(data.values(Indicator.MacdSignal), shapeBottom, config.macdLineOffset)
        val histogram = PointConverter.histogram(data.values(Indicator.MacdHistogram), shapeBottom, config.macdHistogramOffset)

        val coordinates = PointConverter.coordinates(data, shapeMain, config.curveVerticalOffset)
        val points = PointConverter.curve(data.values(Indicator.Candle), shapeMain, config.curveVerticalOffset)
        val volumeBars = PointConverter.volume(data.values(Indicator.Volume), shapeBottom, config.volumeOffset)
        val columns = GridHelper.map(chartType, data.startTimestamp, data.endTimestamp, shapeMain.right)

        chartTouch.configure(config, shapeTimeline.bottom)
        chartTouch.setCoordinates(coordinates)

        emaFastCurve.setShape(shapeMain)
        emaFastCurve.setPoints(emaFast)
        emaFastCurve.setColor(config.curveFastColor)

        emaSlowCurve.setShape(shapeMain)
        emaSlowCurve.setPoints(emaSlow)
        emaSlowCurve.setColor(config.curveSlowColor)

        // macd
        rsiCurve.setShape(shapeBottom)
        rsiCurve.setPoints(rsi)
        rsiCurve.setColor(config.curveFastColor)

        // macd
        macdCurve.setShape(shapeBottom)
        macdCurve.setPoints(macd)
        macdCurve.setColor(config.curveFastColor)

        macdSignalCurve.setShape(shapeBottom)
        macdSignalCurve.setPoints(macdSignal)
        macdSignalCurve.setColor(config.curveSlowColor)

        macdHistogram.setShape(shapeBottom)
        macdHistogram.setPoints(histogram)

        mainCurve.setShape(shapeMain)
        mainCurve.setPoints(points)
        mainCurve.setColor(config.curveColor)

        mainGradient.setPoints(points)
        mainGradient.setShape(shapeMain)
        mainGradient.setShader(config.curveColor)

        bottomVolume.setPoints(volumeBars)
        bottomVolume.setShape(shapeBottom)

        gridMain.setShape(shapeMain)
        gridMain.set(columns)

        gridBottom.setShape(shapeBottom)
        gridBottom.set(columns)

        val candleRange = data.valueRange
        gridDashMain.setShape(shapeMain)
        gridDashMain.setValues(formatRate(candleRange.upper), formatRate(candleRange.lower))

        gridDashBottom.setShape(shapeBottom)
        gridDashBottom.setOffset(shapeBottom.height() * 0.3f)
        gridDashBottom.setValues("70", "30")

        gridTimeline.setColumns(columns)
        gridTimeline.setShape(shapeTimeline)

        // ---------------------------
        // *********
        // ---------------------------

        chartMain.clear()
        chartMain.add(mainCurve, mainGradient)
        chartMain.add(gridMain, gridDashMain)
        chartMain.add(emaFastCurve, emaSlowCurve)

        chartBottom.clear()
        chartBottom.add(gridBottom)
        chartBottom.add(bottomVolume)
//        chartBottom.add(macdHistogram, macdCurve, macdSignalCurve)
//        chartBottom.add(rsiCurve, gridDashBottom)

        chartTimeline.clear()
        chartTimeline.add(gridTimeline)

        animator.start()
    }

    private fun formatRate(value: Float): String {
        return rateFormatter?.format(value.toBigDecimal()) ?: value.toString()
    }
}
