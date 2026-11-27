package io.horizontalsystems.walletkit.modules.coin.overview.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartTransition
import io.horizontalsystems.chartview.CurveAnimator2
import io.horizontalsystems.chartview.CurveAnimatorBars
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.ChartVolume
import io.horizontalsystems.designkit.theme.Colors
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.absoluteValue

class ChartHelper(
    private var target: ChartData,
    var hasVolumes: Boolean,
    private val colors: Colors,
    private val transition: ChartTransition,
    private val appear: ChartTransition,
    private val show: ChartTransition,
    private val hide: ChartTransition,
) {

    private var minValue: Float = 0.0f
    private var maxValue: Float = 0.0f
    private var minKey: Long = 0
    private var maxKey: Long = 0

    private val mainCurve: CurveAnimator2
    private var dominanceCurve: CurveAnimator2? = null
    private val movingAverageCurves = mutableMapOf<String, CurveAnimator2>()
    private var volumeBars: CurveAnimatorBars? = null
    private var rsiCurve: CurveAnimator2? = null
    private var macdLineCurve: CurveAnimator2? = null
    private var macdSignalCurve: CurveAnimator2? = null
    private var macdHistogramBars: CurveAnimatorBars? = null

    // Layers whose data was taken away stay until they have played the hide transition.
    private val leavingMovingAverageCurves = mutableMapOf<String, CurveAnimator2>()
    private var dominanceLeaving = false
    private var rsiLeaving = false
    private var macdLeaving = false

    // RSI and MACD take the panel of the volume bars, which leave and come back like a layer
    // but keep their data meanwhile.
    private var volumeBarsLeaving = false
    private var volumeBarsCovered = false

    // Which layers exist is not snapshot state by itself. Reading this where they are handed
    // out redraws the chart when one is dropped on a frame that changes no other layer.
    private var layersVersion by mutableIntStateOf(0)

    // The colours a data change leaves and the ones it moves to. The curve is drawn in a blend
    // of the two that follows the same fraction as its points.
    private var mainCurveColorsFrom: MainCurveColors
    private var mainCurveColorsTo: MainCurveColors

    var mainCurveColor by mutableStateOf(colors.greenD)
        private set
    var mainCurveGradientColors by mutableStateOf(Pair(Color(0x00416BFF), Color(0x8013D670)))
        private set
    var mainCurvePressedColor = colors.leah
    var mainCurveGradientPressedColors = Pair(colors.leah.copy(alpha = 0f), colors.leah.copy(alpha = 0.5f))
    var mainBarsColor = colors.orange
    var mainBarsPressedColor = colors.andy

    init {
        setExtremum()

        mainCurve = CurveAnimator2(
            target.valuesByTimestamp(),
            minKey,
            maxKey,
            minValue,
            maxValue,
            transition,
            appear
        )

        initDominanceCurve(appear)

        initMovingAverages()

        initRsiCurve(appear)
        initMacdCurves(appear)

        if (hasVolumes) {
            val volumeByTimestamp = target.volumeByTimestamp()
            // hasVolumes says the chart type has a volume track, not that this response carried
            // any: every point can come back with a null volume, and minOf throws on an empty
            // map. setValues() below already guards this — the initial build did not.
            if (volumeByTimestamp.isNotEmpty()) {
                volumeBars = CurveAnimatorBars(
                    volumeByTimestamp,
                    minKey,
                    maxKey,
                    volumeByTimestamp.minOf { it.value },
                    volumeByTimestamp.maxOf { it.value },
                    transition,
                    appear
                )
            }
        }

        volumeBarsCovered = hasBottomIndicator

        mainCurveColorsTo = targetColors()
        mainCurveColorsFrom = mainCurveColorsTo
        showColors(mainCurveColorsTo)
    }

    private fun initDominanceCurve(appear: ChartTransition) {
        val dominanceValues = target.dominanceByTimestamp()
        if (dominanceValues.isNotEmpty()) {
            dominanceCurve = CurveAnimator2(
                dominanceValues,
                minKey,
                maxKey,
                dominanceValues.minOf { it.value },
                dominanceValues.maxOf { it.value },
                transition,
                appear
            )
        }
    }

    private fun targetColors(): MainCurveColors {
        val chartData = target

        return when {
            chartData.disabled -> MainCurveColors(
                curve = colors.grey,
                gradient = Pair(colors.grey.copy(alpha = 0f), colors.grey.copy(alpha = 0.5f))
            )

            !chartData.isMovementChart -> MainCurveColors(
                curve = colors.orange,
                gradient = Pair(Color(0x00FE4A11), Color(0x80FE4A11))
            )

            chartData.diff() < BigDecimal.ZERO -> MainCurveColors(
                curve = colors.redD,
                gradient = Pair(Color(0x007413D6), Color(0x80FF0303))
            )

            else -> MainCurveColors(
                curve = colors.greenD,
                gradient = Pair(Color(0x00416BFF), Color(0x8013D670))
            )
        }
    }

    private fun showColors(colors: MainCurveColors) {
        mainCurveColor = colors.curve
        mainCurveGradientColors = colors.gradient
    }

    private fun initMacdCurves(appear: ChartTransition) {
        val macd = target.macd
        if (macd != null) {
            val macdLine = macd.macdLine
            val signalLine = macd.signalLine

            val extremum = listOf(
                macdLine.minOfOrNull { it.value },
                macdLine.maxOfOrNull { it.value },
                signalLine.minOfOrNull { it.value },
                signalLine.maxOfOrNull { it.value },
            )
            val absMax = extremum.mapNotNull { it?.absoluteValue }.maxOrNull() ?: 0f
            val absMin = absMax * -1

            macdLineCurve = CurveAnimator2(
                macdLine,
                minKey,
                maxKey,
                absMin,
                absMax,
                transition,
                appear
            )
            macdSignalCurve = CurveAnimator2(
                signalLine,
                minKey,
                maxKey,
                absMin,
                absMax,
                transition,
                appear
            )
            val histogram = macd.histogram
            val histogramAbsMax = histogram.values.maxOfOrNull { it.absoluteValue } ?: 0f
            val histogramAbsMin = histogramAbsMax * -1
            macdHistogramBars = CurveAnimatorBars(
                histogram,
                minKey,
                maxKey,
                histogramAbsMin,
                histogramAbsMax,
                transition,
                appear
            )
        }
    }

    private fun initRsiCurve(appear: ChartTransition) {
        rsiCurve = target.rsi?.let { chartIndicatorRsi ->
            val values = chartIndicatorRsi.points
            // Too few points, or all of them filtered out, leaves this empty and minOf throws.
            // The update path checks points.isNullOrEmpty() for the same reason.
            if (values.isEmpty()) return@let null

            CurveAnimator2(
                values,
                minKey,
                maxKey,
                RSI_MIN,
                RSI_MAX,
                transition,
                appear
            )
        }
    }

    private fun initMovingAverages() {
        movingAverageCurves.clear()
        target.movingAverages.forEach { (id, movingAverage) ->
            movingAverageCurves[id] = movingAverageCurve(movingAverage, appear)
        }
    }

    private fun movingAverageCurve(
        movingAverage: ChartIndicator.MovingAverage,
        appear: ChartTransition,
    ) = CurveAnimator2(
        movingAverage.line,
        minKey,
        maxKey,
        minValue,
        maxValue,
        transition,
        appear
    ).apply {
        color = movingAverage.color
    }

    private fun dropLeavingLayers() {
        leavingMovingAverageCurves.clear()
        if (dominanceLeaving) {
            dominanceCurve = null
            dominanceLeaving = false
        }
        if (rsiLeaving) {
            rsiCurve = null
            rsiLeaving = false
        }
        if (macdLeaving) {
            macdLineCurve = null
            macdSignalCurve = null
            macdHistogramBars = null
            macdLeaving = false
        }
        if (volumeBarsLeaving) {
            volumeBarsCovered = true
            volumeBarsLeaving = false
        }
        layersVersion++
    }

    /**
     * Whether RSI or MACD holds the panel under the chart. One that is leaving does not, so
     * the volume bars come back under it while it goes.
     */
    private val hasBottomIndicator: Boolean
        get() = (rsiCurve != null && !rsiLeaving) || (macdLineCurve != null && !macdLeaving)

    // RSI and MACD share one panel. With both in the data, RSI has it. One that is leaving
    // is still drawn under or over the other until its fade ends.
    private val macdShown: Boolean
        get() = rsiCurve == null || rsiLeaving || macdLeaving

    fun getMainCurveState(): CurveAnimator2.UiState {
        return mainCurve.state
    }

    fun getDominanceCurveState(): CurveAnimator2.UiState? {
        layersVersion
        return dominanceCurve?.state
    }

    fun getVolumeBarsState(): CurveAnimatorBars.UiState? {
        layersVersion
        return volumeBars?.state.takeUnless { volumeBarsCovered }
    }

    fun getMovingAverageCurveStates(): List<CurveAnimator2.UiState> {
        layersVersion
        return (movingAverageCurves.values + leavingMovingAverageCurves.values).map { it.state }
    }

    fun getRsiCurveState(): CurveAnimator2.UiState? {
        layersVersion
        return rsiCurve?.state
    }

    fun getMacdLineCurveState(): CurveAnimator2.UiState? {
        layersVersion
        return macdLineCurve?.state.takeIf { macdShown }
    }

    fun getMacdSignalCurveState(): CurveAnimator2.UiState? {
        layersVersion
        return macdSignalCurve?.state.takeIf { macdShown }
    }

    fun getMacdHistogramBarsState(): CurveAnimatorBars.UiState? {
        layersVersion
        return macdHistogramBars?.state.takeIf { macdShown }
    }

    private fun setExtremum() {
        minValue = target.minValue
        maxValue = target.maxValue

        if (minValue == maxValue) {
            minValue *= 0.9f
        }

        minKey = target.startTimestamp
        maxKey = target.endTimestamp

        if (minKey == maxKey) {
            minKey = (minKey * 0.9).toLong()
            maxKey = (maxKey * 1.1).toLong()
        }
    }

    fun setTarget(chartData: ChartData, hasVolumes: Boolean) {
        // A layer still leaving when new data comes goes on from the opacity it has: on with
        // leaving if its data is still absent, back in if it has returned.
        val hadBottomIndicator = hasBottomIndicator
        val macdWasShown = macdShown
        (leavingMovingAverageCurves.keys - chartData.movingAverages.keys).forEach { id ->
            leavingMovingAverageCurves[id]?.leave(hide)
        }

        target = chartData
        this.hasVolumes = hasVolumes
        setExtremum()

        mainCurve.setTo(
            target.valuesByTimestamp(),
            minKey,
            maxKey,
            minValue,
            maxValue
        )

        (movingAverageCurves.keys - target.movingAverages.keys).forEach { id ->
            movingAverageCurves.remove(id)?.let { curve ->
                curve.leave(hide)
                leavingMovingAverageCurves[id] = curve
            }
        }
        target.movingAverages.forEach { (id, movingAverage) ->
            // One that returns while it is still leaving is taken back as it is on screen.
            val curve = movingAverageCurves[id] ?: leavingMovingAverageCurves.remove(id)?.also {
                it.color = movingAverage.color
                movingAverageCurves[id] = it
            }
            if (curve == null) {
                movingAverageCurves[id] = movingAverageCurve(movingAverage, show)
            } else {
                curve.setTo(
                    movingAverage.line,
                    minKey,
                    maxKey,
                    minValue,
                    maxValue,
                )
            }
        }

        val dominanceValues = target.dominanceByTimestamp()
        if (dominanceValues.isEmpty()) {
            dominanceCurve?.let {
                it.leave(hide)
                dominanceLeaving = true
            }
        } else if (dominanceCurve == null) {
            initDominanceCurve(show)
        } else {
            dominanceCurve?.setTo(
                dominanceValues,
                minKey,
                maxKey,
                dominanceValues.minOf { it.value },
                dominanceValues.maxOf { it.value }
            )
            dominanceLeaving = false
        }

        if (target.rsi == null || target.rsi?.points.isNullOrEmpty()) {
            rsiCurve?.let {
                it.leave(hide)
                rsiLeaving = true
            }
        } else if (rsiCurve == null) {
            initRsiCurve(show)
        } else {
            target.rsi?.let { chartIndicatorRsi ->
                val values = chartIndicatorRsi.points
                rsiCurve?.setTo(
                    values,
                    minKey,
                    maxKey,
                    RSI_MIN,
                    RSI_MAX
                )
                rsiLeaving = false
            }
        }

        if (target.macd == null) {
            if (macdLineCurve != null || macdSignalCurve != null || macdHistogramBars != null) {
                if (macdWasShown) {
                    macdLineCurve?.leave(hide)
                    macdSignalCurve?.leave(hide)
                    macdHistogramBars?.leave(hide)
                    macdLeaving = true
                } else {
                    // It was behind RSI, so there is nothing on screen to fade.
                    macdLineCurve = null
                    macdSignalCurve = null
                    macdHistogramBars = null
                }
            }
        } else if (macdLineCurve == null || macdSignalCurve == null || macdHistogramBars == null) {
            initMacdCurves(show)
        } else {
            target.macd?.let { macd ->
                val macdLine = macd.macdLine
                val signalLine = macd.signalLine

                val extremum = listOf(
                    macdLine.minOfOrNull { it.value },
                    macdLine.maxOfOrNull { it.value },
                    signalLine.minOfOrNull { it.value },
                    signalLine.maxOfOrNull { it.value },
                )
                val absMax = extremum.mapNotNull { it?.absoluteValue }.maxOrNull() ?: 0f
                val absMin = absMax * -1

                macdLineCurve?.setTo(
                    macdLine,
                    minKey,
                    maxKey,
                    absMin,
                    absMax
                )
                macdSignalCurve?.setTo(
                    signalLine,
                    minKey,
                    maxKey,
                    absMin,
                    absMax
                )
                val histogram = macd.histogram
                val histogramAbsMax = histogram.values.maxOfOrNull { it.absoluteValue } ?: 0f
                val histogramAbsMin = histogramAbsMax * -1
                macdHistogramBars?.setValues(
                    histogram,
                    minKey,
                    maxKey,
                    histogramAbsMin,
                    histogramAbsMax
                )
                macdLeaving = false
            }
        }

        // The volume bars were hidden behind the indicator that is going, so they come in anew.
        if (hadBottomIndicator && !hasBottomIndicator) {
            volumeBars?.reappear(show, stillShown = volumeBarsLeaving)
            volumeBarsCovered = false
            volumeBarsLeaving = false
        }

        val volumeByTimestamp = if (hasVolumes) target.volumeByTimestamp() else linkedMapOf()
        when {
            // Dropping the bars matters as much as creating them: a response without volumes must
            // not leave the previous chart's bars on screen.
            volumeByTimestamp.isEmpty() -> volumeBars = null

            // The first response can legitimately carry no volumes, which leaves no animator to
            // update, so a later populated response has to be able to create one.
            volumeBars == null -> volumeBars = CurveAnimatorBars(
                volumeByTimestamp,
                minKey,
                maxKey,
                volumeByTimestamp.minOf { it.value },
                volumeByTimestamp.maxOf { it.value },
                transition,
                show
            )

            else -> volumeBars?.setValues(
                volumeByTimestamp,
                minKey,
                maxKey,
                volumeByTimestamp.minOf { it.value },
                volumeByTimestamp.maxOf { it.value }
            )
        }

        // After the bars took the new data, so that it is the hide transition they play. Bars
        // already leaving go on with it from the opacity they have.
        if (hasBottomIndicator && !volumeBarsCovered) {
            volumeBars?.leave(hide)
            volumeBarsLeaving = true
        }

        layersVersion++

        // From the colours on screen, which are a blend when a transition was interrupted.
        mainCurveColorsFrom = MainCurveColors(mainCurveColor, mainCurveGradientColors)
        mainCurveColorsTo = targetColors()
    }

    fun onNextFrame(value: Float) {
        // A colour has no room to overshoot, so the fraction of a bouncing spec is held in 0..1.
        val colorFraction = value.coerceIn(0f, 1f)
        showColors(
            MainCurveColors(
                curve = lerp(mainCurveColorsFrom.curve, mainCurveColorsTo.curve, colorFraction),
                gradient = Pair(
                    lerp(mainCurveColorsFrom.gradient.first, mainCurveColorsTo.gradient.first, colorFraction),
                    lerp(mainCurveColorsFrom.gradient.second, mainCurveColorsTo.gradient.second, colorFraction),
                )
            )
        )

        mainCurve.onNextFrame(value)
        dominanceCurve?.onNextFrame(value)
        movingAverageCurves.forEach { (_, curve) ->
            curve.onNextFrame(value)
        }
        leavingMovingAverageCurves.forEach { (_, curve) ->
            curve.onNextFrame(value)
        }
        volumeBars?.onNextFrame(value)
        rsiCurve?.onNextFrame(value)
        macdLineCurve?.onNextFrame(value)
        macdSignalCurve?.onNextFrame(value)
        macdHistogramBars?.onNextFrame(value)

        if (value == 1f) {
            dropLeavingLayers()
        }
    }

    var selectedItem by mutableStateOf<SelectedItem?>(null)
        private set

    fun setSelectedPercentagePositionX(percentagePositionX: Float?) {
        if (percentagePositionX == null) {
            selectedItem = null
            return
        }

        val interval = maxKey - minKey
        val timestamp = minKey + interval * percentagePositionX

        val nearestChartPoint = target.items.minByOrNull {
            abs(it.timestamp - timestamp)
        } ?: return

        val selectedTimestamp = nearestChartPoint.timestamp

        val selectedMovingAverages = mutableListOf<SelectedItem.MA>()
        target.movingAverages.forEach { _, movingAverage ->
            movingAverage.line[selectedTimestamp]?.let {
                selectedMovingAverages.add(SelectedItem.MA(movingAverage.color, it))
            }
        }
        val selectedRsi = target.rsi?.let {
            it.points[selectedTimestamp]
        }
        val selectedMacd = target.macd?.let {
            val macd = it.macdLine[selectedTimestamp]
            val signal = it.signalLine[selectedTimestamp]
            val histogram = it.histogram[selectedTimestamp]
            if (macd != null) {
                SelectedItem.Macd(macd, signal, histogram)
            } else {
                null
            }
        }

        val nearestPercentagePositionX = (nearestChartPoint.timestamp - minKey) / interval.toFloat()

        selectedItem = SelectedItem(
            percentagePositionX = nearestPercentagePositionX,
            timestamp = selectedTimestamp,
            mainValue = nearestChartPoint.value,
            dominance = nearestChartPoint.dominance,
            volume = nearestChartPoint.chartVolume,
            movingAverages = selectedMovingAverages,
            rsi = selectedRsi,
            macd = selectedMacd,
        )
    }

}

// The range the RSI panel is drawn in, whatever the values in the data.
private const val RSI_MIN = 0f
private const val RSI_MAX = 100f

private data class MainCurveColors(val curve: Color, val gradient: Pair<Color, Color>)

data class SelectedItem(
    val percentagePositionX: Float,
    val timestamp: Long,
    val mainValue: Float,
    val dominance: Float?,
    val volume: ChartVolume?,
    val movingAverages: List<MA>,
    val rsi: Float?,
    val macd: Macd?
) {
    data class MA(val color: Long, val value: Float)
    data class Macd(val macdValue: Float, val signalValue: Float?, val histogramValue: Float?)
}
