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
    var isVisible: Boolean
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
    private val animatorMain = ChartAnimator { chartMain.invalidate() }
    private val animatorBottom = ChartAnimator { chartBottom.invalidate() }

    private val mainCurve = ChartCurve(config, animatorMain, isVisible = true)
    private val mainGradient = ChartGradient(animatorMain)

    private val mainGrid = ChartGrid(config)
    private val mainRange = ChartGridRange(config)

    private val bottomGrid = ChartGrid(config)
    private val bottomVolume = ChartVolume(config, animatorBottom)
    private val timelineGrid = ChartGridTimeline(config)

    private val emaFastCurve = ChartCurve(config, animatorMain)
    private val emaSlowCurve = ChartCurve(config, animatorMain)
    private val emaLabel = ChartBottomLabel(config)

    private val macdCurve = ChartCurve(config, animatorBottom)
    private val macdSignal = ChartCurve(config, animatorBottom)
    private val macdHistogram = ChartHistogram(config, animatorBottom)
    private val macdLabel = ChartBottomLabel(config)

    private val rsiCurve = ChartCurve(config, animatorBottom)
    private val rsiRange = ChartGridRange(config, isVisible = false)

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
        listOf(chartMain, chartBottom, chartTimeline).forEach {
            it.visibility = View.INVISIBLE
        }
        chartError.showIf(true)
        chartError.text = error
    }

    fun showEma() {
        setVisible(emaFastCurve, emaSlowCurve, emaLabel, isVisible = !emaFastCurve.isVisible)
        animatorMain.start()
    }

    fun showMacd() {
        val visible = !macdCurve.isVisible
        setVisible(macdCurve, macdSignal, macdHistogram, macdLabel, isVisible = visible)
        setVisible(rsiCurve, rsiRange, isVisible = false)
        setVisible(bottomVolume, isVisible = !visible)
        animatorBottom.start()
    }

    fun showRsi() {
        val visible = !rsiCurve.isVisible
        setVisible(rsiCurve, rsiRange, isVisible = visible)
        setVisible(macdCurve, macdSignal, macdHistogram, macdLabel, isVisible = false)
        setVisible(bottomVolume, isVisible = !visible)
        animatorBottom.start()
    }

    fun setData(data: ChartData, chartType: ChartView.ChartType) {
        config.setTrendColor(data)

        val emaFast = PointConverter.curve(data.values(Indicator.EmaFast), chartMain.shape, config.curveVerticalOffset)
        val emaSlow = PointConverter.curve(data.values(Indicator.EmaSlow), chartMain.shape, config.curveVerticalOffset)
        val rsi = PointConverter.curve(data.values(Indicator.Rsi), chartBottom.shape, 0f)

        val macd = PointConverter.curve(data.values(Indicator.Macd), chartBottom.shape, config.macdLineOffset)
        val signal = PointConverter.curve(data.values(Indicator.MacdSignal), chartBottom.shape, config.macdLineOffset)
        val histogram = PointConverter.histogram(data.values(Indicator.MacdHistogram), chartBottom.shape, config.macdHistogramOffset)

        val coordinates = PointConverter.coordinates(data, chartMain.shape, config.curveVerticalOffset)
        val points = PointConverter.curve(data.values(Indicator.Candle), chartMain.shape, config.curveVerticalOffset)
        val volumes = PointConverter.volume(data.values(Indicator.Volume), chartBottom.shape, config.volumeOffset)
        val timeline = GridHelper.map(chartType, data.startTimestamp, data.endTimestamp, chartMain.shape.right)

        chartTouch.configure(config, chartTimeline.shape.height())
        chartTouch.setCoordinates(coordinates)

        // EMA
        emaFastCurve.setShape(chartMain.shape)
        emaFastCurve.setPoints(emaFast)
        emaFastCurve.setColor(config.curveFastColor)

        emaSlowCurve.setShape(chartMain.shape)
        emaSlowCurve.setPoints(emaSlow)
        emaSlowCurve.setColor(config.curveSlowColor)

        emaLabel.setShape(chartMain.shape)
        emaLabel.setValues(mapOf(
                "50" to config.curveSlowColor,
                "25" to config.curveFastColor))

        // RSI
        rsiCurve.setShape(chartBottom.shape)
        rsiCurve.setPoints(rsi)
        rsiCurve.setColor(config.curveFastColor)

        rsiRange.setShape(chartBottom.shape)
        rsiRange.setOffset(chartBottom.shape.height() * 0.3f)
        rsiRange.setValues("70", "30")

        // MACD
        macdCurve.setShape(chartBottom.shape)
        macdCurve.setPoints(macd)
        macdCurve.setColor(config.curveFastColor)

        macdSignal.setShape(chartBottom.shape)
        macdSignal.setPoints(signal)
        macdSignal.setColor(config.curveSlowColor)

        macdHistogram.setShape(chartBottom.shape)
        macdHistogram.setPoints(histogram)

        macdLabel.setShape(chartBottom.shape)
        macdLabel.setOffset(chartBottom.shape.height() * 0.3f)
        macdLabel.setValues(mapOf(
                "9" to config.gridLabelColor,
                "26" to config.gridLabelColor,
                "12" to config.gridLabelColor))

        // Candles
        mainCurve.setShape(chartMain.shape)
        mainCurve.setPoints(points)
        mainCurve.setColor(config.curveColor)

        mainGradient.setPoints(points)
        mainGradient.setShape(chartMain.shape)
        mainGradient.setShader(config.curveColor)

        mainGrid.setShape(chartMain.shape)
        mainGrid.set(timeline)

        val candleRange = data.valueRange
        mainRange.setShape(chartMain.shape)
        mainRange.setValues(formatRate(candleRange.upper), formatRate(candleRange.lower))

        // Volume
        bottomVolume.setPoints(volumes)
        bottomVolume.setShape(chartBottom.shape)

        bottomGrid.setShape(chartBottom.shape)
        bottomGrid.set(timeline)

        // Timeline
        timelineGrid.setColumns(timeline)
        timelineGrid.setShape(chartTimeline.shape)

        // ---------------------------
        // *********
        // ---------------------------

        chartMain.clear()
        chartMain.add(mainCurve, mainGradient)
        chartMain.add(mainGrid, mainRange, emaLabel)
        chartMain.add(emaFastCurve, emaSlowCurve)

        chartBottom.clear()
        chartBottom.add(bottomGrid)
        chartBottom.add(bottomVolume)
        chartBottom.add(macdHistogram, macdCurve, this.macdSignal, macdLabel)
        chartBottom.add(rsiCurve, rsiRange)

        chartTimeline.clear()
        chartTimeline.add(timelineGrid)

        animatorMain.start()
        animatorBottom.start()
    }

    private fun setVisible(vararg draw: ChartDraw, isVisible: Boolean) {
        draw.forEach { it.isVisible = isVisible }
    }

    private fun formatRate(value: Float): String {
        return rateFormatter?.format(value.toBigDecimal()) ?: value.toString()
    }
}
