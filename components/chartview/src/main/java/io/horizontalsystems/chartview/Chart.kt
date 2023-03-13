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
    private val animatorMain = ChartAnimator {
        binding.chartMain.invalidate()
        binding.chartGrid.invalidate()
    }
    private val animatorBottom = ChartAnimator { binding.chartBottom.invalidate() }
    private val animatorTopBottomRange = ChartAnimator { binding.topLowRange.invalidate() }

    private val mainCurve = ChartCurve2(config)
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

    private val dominanceCurve = ChartCurve2(config)
    private val dominanceLabel = ChartBottomLabel(config)

    private var mainCurveAnimator: CurveAnimator? = null
    private var dominanceCurveAnimator: CurveAnimator? = null

    init {
        animatorMain.addUpdateListener {
            mainCurveAnimator?.nextFrame(animatorMain.animatedFraction)
            dominanceCurveAnimator?.nextFrame(animatorMain.animatedFraction)
        }
    }

    fun setIndicatorLineVisible(v: Boolean) {
        binding.chartBottom.isVisible = v
    }

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

    fun hideError() {
        showChart(true)
        binding.chartError.isVisible = false
        binding.chartError.text = null
    }

    fun showChart(visible: Boolean = true) {
        setVisible(
            binding.chartMain,
            binding.topLowRange,
            binding.chartTimeline,
            isVisible = visible
        )
    }

    fun setData(
        data: ChartData,
        maxValue: String?,
        minValue: String?
    ) {

        animatorMain.cancel()

        val candleValues = data.valuesByTimestamp(Candle)
        val minCandleValue = candleValues.values.minOrNull() ?: 0f
        val maxCandleValue = candleValues.values.maxOrNull() ?: 0f

        mainCurveAnimator = CurveAnimator(
            candleValues,
            data.startTimestamp,
            data.endTimestamp,
            minCandleValue,
            maxCandleValue,
            mainCurveAnimator,
            binding.chartMain.shape.right,
            binding.chartMain.shape.bottom,
            config.curveVerticalOffset,
            config.curveVerticalOffset,
        )

        config.setTrendColor(data)

        val coordinates =
            PointConverter.coordinates(data, binding.chartMain.shape, config.curveVerticalOffset)
        val volumes = PointConverter.volume(
            data.values(Volume),
            binding.chartBottom.shape,
            config.volumeOffset
        )
        val timeline = GridHelper.map(
            data.startTimestamp,
            data.endTimestamp,
            binding.chartMain.shape.right
        )

        //Dominance
        val dominanceValues = data.valuesByTimestamp(Dominance)
        if (dominanceValues.isNotEmpty()) {
            dominanceCurveAnimator = CurveAnimator(
                dominanceValues,
                data.startTimestamp,
                data.endTimestamp,
                dominanceValues.values.minOrNull() ?: 0f,
                dominanceValues.values.maxOrNull() ?: 0f,
                dominanceCurveAnimator,
                binding.chartMain.shape.right,
                binding.chartMain.shape.bottom,
                config.curveVerticalOffset,
                config.curveVerticalOffset,
            )

            dominanceCurve.setShape(binding.chartMain.shape)
            dominanceCurve.setCurveAnimator(dominanceCurveAnimator!!)
            dominanceCurve.setColor(config.curveSlowColor)

            dominanceLabel.setShape(binding.chartMain.shape)
            val dValues = dominanceValues.values
            val dominancePercent = decimalFormat.format(dValues.last())
            val diff = dValues.last() - dValues.first()
            val diffColor = if (diff > 0f) config.trendUpColor else config.trendDownColor
            val sign = when {
                diff > 0f -> "+"
                diff < 0f -> "-"
                else -> ""
            }
            val diffFormatted = sign + decimalFormat.format(diff)
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

        // Candles
        mainCurve.setShape(binding.chartMain.shape)
        mainCurve.setCurveAnimator(mainCurveAnimator!!)
        mainCurve.setColor(config.curveColor)

        mainGradient.setCurveAnimator(mainCurveAnimator!!)
        mainGradient.setShape(binding.chartMain.shape)
        mainGradient.setShader(config.curveGradient)

        mainGrid.setShape(binding.chartGrid.shape)
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
        binding.chartMain.add(emaLabel, dominanceLabel)
        binding.chartMain.add(emaFastCurve, emaSlowCurve)
        binding.chartMain.add(dominanceCurve)

        binding.chartGrid.clear()
        binding.chartGrid.add(mainGrid)

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

    private fun setVisible(vararg view: View, isVisible: Boolean) {
        view.forEach { it.isVisible = isVisible }
    }

    companion object {
        private val decimalFormat = DecimalFormat("#.##")
    }

}
