package io.horizontalsystems.chartview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.chartview.Indicator.*
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.helpers.GridHelper
import io.horizontalsystems.chartview.helpers.PointConverter
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.PointInfo
import kotlinx.android.synthetic.main.view_chart.view.*
import java.math.BigDecimal

class Chart @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onTouchDown()
        fun onTouchUp()
        fun onTouchSelect(point: PointInfo)
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
    private val animatorTopBottomRange = ChartAnimator { topLowRange.invalidate() }

    private val mainCurve = ChartCurve(config, animatorMain, isVisible = true)
    private val mainGradient = ChartGradient(animatorMain)

    private val mainGrid = ChartGrid(config)
    private val mainRange = ChartGridRange(config)

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

            override fun onTouchSelect(point: PointInfo) {
                listener.onTouchSelect(point)
            }
        })
    }

    fun showSinner() {
        chartError.isVisible = false
        chartViewSpinner.isVisible = true
        loadingShade.isVisible = true
    }

    fun hideSinner() {
        chartViewSpinner.isVisible = false
        loadingShade.isVisible = false
    }

    fun showError(error: String) {
        showChart(false)
        chartError.isVisible = true
        chartError.text = error
    }

    fun setIndicator(indicator: ChartIndicator, visible: Boolean){
        if (visible){
            hideOtherIndicators(indicator)
        }
        when(indicator){
            ChartIndicator.Ema -> {
                setVisible(emaFastCurve, emaSlowCurve, emaLabel, isVisible = visible)
                animatorMain.start()
            }
            ChartIndicator.Macd -> setVisible(macdCurve, macdSignal, macdHistogram, macdLabel, isVisible = visible)
            ChartIndicator.Rsi -> setVisible(rsiCurve, rsiRange, isVisible = visible)
        }

        setVisible(bottomVolume, mainRange, isVisible = !visible)

        animatorTopBottomRange.start()
        animatorBottom.start()
    }

    private fun hideOtherIndicators(indicator: ChartIndicator) {
        ChartIndicator.values().filter { it != indicator }.forEach {
            if (it == ChartIndicator.Ema && emaFastCurve.isVisible){
                setVisible(emaFastCurve, emaSlowCurve, emaLabel, isVisible = false)
                animatorMain.start()
            }
            if (it == ChartIndicator.Macd && macdCurve.isVisible){
                setVisible(macdCurve, macdSignal, macdHistogram, macdLabel, isVisible = false)
            }
            if (it == ChartIndicator.Rsi && rsiCurve.isVisible){
                setVisible(rsiCurve, rsiRange, isVisible = false)
            }
        }
    }

    fun showChart(visible: Boolean = true) {
        setVisible(chartMain, topLowRange, chartBottom, chartTimeline, isVisible = visible)
    }

    fun setData(data: ChartData, chartType: ChartView.ChartType) {
        config.setTrendColor(data)

        val emaFast = PointConverter.curve(data.values(EmaFast), chartMain.shape, config.curveVerticalOffset)
        val emaSlow = PointConverter.curve(data.values(EmaSlow), chartMain.shape, config.curveVerticalOffset)
        val rsi = PointConverter.curve(data.values(Rsi), chartBottom.shape, 0f)

        val macd = PointConverter.curve(data.values(Macd), chartBottom.shape, config.macdLineOffset)
        val signal = PointConverter.curve(data.values(MacdSignal), chartBottom.shape, config.macdLineOffset)
        val histogram = PointConverter.histogram(data.values(MacdHistogram), chartBottom.shape, config.macdHistogramOffset)

        val coordinates = PointConverter.coordinates(data, chartMain.shape, config.curveVerticalOffset)
        val points = PointConverter.curve(data.values(Candle), chartMain.shape, config.curveVerticalOffset)
        val volumes = PointConverter.volume(data.values(Volume), chartBottom.shape, config.volumeOffset)
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
                EmaSlow.period.toString() to config.curveSlowColor,
                EmaFast.period.toString() to config.curveFastColor))

        // RSI
        rsiCurve.setShape(chartBottom.shape)
        rsiCurve.setPoints(rsi)
        rsiCurve.setColor(config.curveSlowColor)

        rsiRange.setShape(chartBottom.shape)
        rsiRange.setOffset(chartBottom.shape.height() * 0.3f)
        rsiRange.setValues(Rsi.max.toString(), Rsi.min.toString(), true)

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
                Macd.signalPeriod.toString() to config.gridLabelColor,
                Macd.slowPeriod.toString() to config.gridLabelColor,
                Macd.fastPeriod.toString() to config.gridLabelColor))

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

        // Timeline
        timelineGrid.setColumns(timeline)
        timelineGrid.setShape(chartTimeline.shape)

        // ---------------------------
        // *********
        // ---------------------------

        chartMain.clear()
        chartMain.add(mainCurve, mainGradient)
        chartMain.add(mainGrid, emaLabel)
        chartMain.add(emaFastCurve, emaSlowCurve)

        topLowRange.clear()
        topLowRange.add(mainRange)

        chartBottom.clear()
        chartBottom.add(bottomVolume)
        chartBottom.add(macdHistogram, macdCurve, macdSignal, macdLabel)
        chartBottom.add(rsiCurve, rsiRange)

        chartTimeline.clear()
        chartTimeline.add(timelineGrid)
        chartTimeline.invalidate()

        animatorMain.start()
        animatorTopBottomRange.start()
        animatorBottom.start()
    }

    private fun setVisible(vararg draw: ChartDraw, isVisible: Boolean) {
        draw.forEach { it.isVisible = isVisible }
    }

    private fun setVisible(vararg view: View, isVisible: Boolean) {
        view.forEach { it.isVisible = isVisible }
    }

    private fun formatRate(value: Float): String {
        return rateFormatter?.format(value.toBigDecimal()) ?: value.toString()
    }
}
