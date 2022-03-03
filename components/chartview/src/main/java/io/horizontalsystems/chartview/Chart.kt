package io.horizontalsystems.chartview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.chartview.Indicator.*
import io.horizontalsystems.chartview.databinding.ViewChartBinding
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.helpers.GridHelper
import io.horizontalsystems.chartview.helpers.PointConverter
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartIndicator
import java.text.DecimalFormat

class Chart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewChartBinding.inflate(LayoutInflater.from(context), this)

    interface Listener {
        fun onTouchDown()
        fun onTouchUp()
        fun onTouchSelect(item: ChartDataItemImmutable)
    }

    private val config = ChartConfig(context, attrs)
    private val animatorMain = ChartAnimator { binding.chartMain.invalidate() }
    private val animatorBottom = ChartAnimator { binding.chartBottom.invalidate() }
    private val animatorTopBottomRange = ChartAnimator { binding.topLowRange.invalidate() }

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

    private val dominanceCurve = ChartCurve(config, animatorMain)
    private val dominanceLabel = ChartBottomLabel(config)

    fun setListener(listener: Listener) {
        binding.chartTouch.onUpdate(object : Listener {
            override fun onTouchDown() {
                mainCurve.setColor(config.curvePressedColor)
                mainGradient.setShader(config.pressedGradient)
                binding.chartMain.invalidate()
                listener.onTouchDown()
            }

            override fun onTouchUp() {
                mainCurve.setColor(config.curveColor)
                mainGradient.setShader(config.curveGradient)
                binding.chartMain.invalidate()
                listener.onTouchUp()
            }

            override fun onTouchSelect(item: ChartDataItemImmutable) {
                listener.onTouchSelect(item)
            }
        })
    }

    fun showSpinner() {
        binding.chartError.isVisible = false
        binding.chartViewSpinner.isVisible = true
        binding.loadingShade.isVisible = true
    }

    fun hideSpinner() {
        binding.chartViewSpinner.isVisible = false
        binding.loadingShade.isVisible = false
    }

    fun showError(error: String) {
        showChart(false)
        binding.chartError.isVisible = true
        binding.chartError.text = error
    }

    private var indicator: ChartIndicator? = null

    fun hideAllIndicators() {
        indicator?.let {
            setIndicator(it, false)
        }
    }

    fun setIndicator(indicator: ChartIndicator, visible: Boolean) {
        this.indicator = indicator
        if (visible) {
            hideOtherIndicators(indicator)
        }
        when (indicator) {
            ChartIndicator.Ema -> {
                setVisible(emaFastCurve, emaSlowCurve, emaLabel, isVisible = visible)
                animatorMain.start()
            }
            ChartIndicator.Macd -> setVisible(
                macdCurve,
                macdSignal,
                macdHistogram,
                macdLabel,
                isVisible = visible
            )
            ChartIndicator.Rsi -> setVisible(rsiCurve, rsiRange, isVisible = visible)
        }

        setVisible(bottomVolume, mainRange, isVisible = !visible)

        animatorTopBottomRange.start()
        animatorBottom.start()
    }

    private fun hideOtherIndicators(indicator: ChartIndicator) {
        ChartIndicator.values().filter { it != indicator }.forEach {
            if (it == ChartIndicator.Ema && emaFastCurve.isVisible) {
                setVisible(emaFastCurve, emaSlowCurve, emaLabel, isVisible = false)
                animatorMain.start()
            }
            if (it == ChartIndicator.Macd && macdCurve.isVisible) {
                setVisible(macdCurve, macdSignal, macdHistogram, macdLabel, isVisible = false)
            }
            if (it == ChartIndicator.Rsi && rsiCurve.isVisible) {
                setVisible(rsiCurve, rsiRange, isVisible = false)
            }
        }
    }

    fun showChart(visible: Boolean = true) {
        setVisible(
            binding.chartMain,
            binding.topLowRange,
            binding.chartBottom,
            binding.chartTimeline,
            isVisible = visible
        )
    }

    fun setData(
        data: ChartData,
        chartType: ChartView.ChartType,
        maxValue: String?,
        minValue: String?
    ) {
        config.setTrendColor(data)

        val emaFast = PointConverter.curve(
            data.values(EmaFast),
            binding.chartMain.shape,
            config.curveVerticalOffset
        )
        val emaSlow = PointConverter.curve(
            data.values(EmaSlow),
            binding.chartMain.shape,
            config.curveVerticalOffset
        )
        val rsi = PointConverter.curve(data.values(Rsi), binding.chartBottom.shape, 0f)

        val macd = PointConverter.curve(
            data.values(Macd),
            binding.chartBottom.shape,
            config.macdLineOffset
        )
        val signal = PointConverter.curve(
            data.values(MacdSignal),
            binding.chartBottom.shape,
            config.macdLineOffset
        )
        val histogram = PointConverter.histogram(
            data.values(MacdHistogram),
            binding.chartBottom.shape,
            config.macdHistogramOffset
        )

        val coordinates =
            PointConverter.coordinates(data, binding.chartMain.shape, config.curveVerticalOffset)
        val points = PointConverter.curve(
            data.values(Candle),
            binding.chartMain.shape,
            config.curveVerticalOffset
        )
        val pointsMap = PointConverter.curveMap(
            data,
            Candle,
            binding.chartMain.shape,
            config.curveVerticalOffset
        )
        val volumes = PointConverter.volume(
            data.values(Volume),
            binding.chartBottom.shape,
            config.volumeOffset
        )
        val timeline = GridHelper.map(
            chartType,
            data.startTimestamp,
            data.endTimestamp,
            binding.chartMain.shape.right
        )

        //Dominance
        val dominancePoints = data.values(Dominance)
        if (dominancePoints.isNotEmpty()) {
            val dominance = PointConverter.curve(
                dominancePoints,
                binding.chartMain.shape,
                config.curveVerticalOffset
            )

            dominanceCurve.setShape(binding.chartMain.shape)
            dominanceCurve.setPoints(dominance)
            dominanceCurve.setColor(config.curveSlowColor)

            dominanceLabel.setShape(binding.chartMain.shape)
            val dominancePercent = decimalFormat.format(dominancePoints.last().value)
            val diff = dominancePoints.last().value - dominancePoints.first().value
            val diffColor = if (diff > 0f) config.trendUpColor else config.trendDownColor
            val diffFormatted = decimalFormat.format(diff)
            dominanceLabel.setValues(
                mapOf(
                    "$diffFormatted%" to diffColor,
                    "BTC Dominance $dominancePercent%" to config.curveDominanceLabelColor
                )
            )

            dominanceCurve.isVisible = true
            dominanceLabel.isVisible = true
        }

        binding.chartTouch.configure(config, binding.chartTimeline.shape.height())
        binding.chartTouch.setCoordinates(coordinates)

        // EMA
        emaFastCurve.setShape(binding.chartMain.shape)
        emaFastCurve.setPoints(emaFast)
        emaFastCurve.setColor(config.curveFastColor)

        emaSlowCurve.setShape(binding.chartMain.shape)
        emaSlowCurve.setPoints(emaSlow)
        emaSlowCurve.setColor(config.curveSlowColor)

        emaLabel.setShape(binding.chartMain.shape)
        emaLabel.setValues(
            mapOf(
                EmaSlow.period.toString() to config.curveSlowColor,
                EmaFast.period.toString() to config.curveFastColor
            )
        )

        // RSI
        rsiCurve.setShape(binding.chartBottom.shape)
        rsiCurve.setPoints(rsi)
        rsiCurve.setColor(config.curveSlowColor)

        rsiRange.setShape(binding.chartBottom.shape)
        rsiRange.setOffset(binding.chartBottom.shape.height() * 0.3f)
        rsiRange.setValues(Rsi.max.toString(), Rsi.min.toString(), true)

        // MACD
        macdCurve.setShape(binding.chartBottom.shape)
        macdCurve.setPoints(macd)
        macdCurve.setColor(config.curveFastColor)

        macdSignal.setShape(binding.chartBottom.shape)
        macdSignal.setPoints(signal)
        macdSignal.setColor(config.curveSlowColor)

        macdHistogram.setShape(binding.chartBottom.shape)
        macdHistogram.setPoints(histogram)

        macdLabel.setShape(binding.chartBottom.shape)
        macdLabel.setOffset(binding.chartBottom.shape.height() * 0.3f)
        macdLabel.setValues(
            mapOf(
                Macd.signalPeriod.toString() to config.gridLabelColor,
                Macd.slowPeriod.toString() to config.gridLabelColor,
                Macd.fastPeriod.toString() to config.gridLabelColor
            )
        )

        // Candles
        mainCurve.setShape(binding.chartMain.shape)
        mainCurve.setPointsMap(pointsMap, data.startTimestamp, data.endTimestamp)
        mainCurve.setColor(config.curveColor)

//        mainGradient.setPoints(points)
//        mainGradient.setShape(binding.chartMain.shape)
//        mainGradient.setShader(config.curveGradient)

        mainGrid.setShape(binding.chartMain.shape)
        mainGrid.set(timeline)

        mainRange.setShape(binding.chartMain.shape)
        mainRange.setValues(maxValue, minValue)

        // Volume
        bottomVolume.setPoints(volumes)
        bottomVolume.setShape(binding.chartBottom.shape)

        // Timeline
        timelineGrid.setColumns(timeline)
        timelineGrid.setShape(binding.chartTimeline.shape)

        // ---------------------------
        // *********
        // ---------------------------

        binding.chartMain.clear()
        binding.chartMain.add(mainCurve, mainGradient)
        binding.chartMain.add(mainGrid, emaLabel, dominanceLabel)
        binding.chartMain.add(emaFastCurve, emaSlowCurve)
        binding.chartMain.add(dominanceCurve)

        binding.topLowRange.clear()
        binding.topLowRange.add(mainRange)

        binding.chartBottom.clear()
        binding.chartBottom.add(bottomVolume)
        binding.chartBottom.add(macdHistogram, macdCurve, macdSignal, macdLabel)
        binding.chartBottom.add(rsiCurve, rsiRange)

        binding.chartTimeline.clear()
        binding.chartTimeline.add(timelineGrid)
        binding.chartTimeline.invalidate()

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

    companion object {
        private val decimalFormat = DecimalFormat("#.##")
    }

}
