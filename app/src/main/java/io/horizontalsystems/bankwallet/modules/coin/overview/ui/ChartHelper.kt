package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.CurveAnimator2
import io.horizontalsystems.chartview.CurveAnimatorBars
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.ChartPoint
import kotlin.math.abs

class ChartHelper(private var target: ChartData, var hasVolumes: Boolean) {

    private var minValue: Float = 0.0f
    private var maxValue: Float = 0.0f
    private var minKey: Long = 0
    private var maxKey: Long = 0

    private val mainCurve: CurveAnimator2
    private val movingAverageCurves = mutableMapOf<String, CurveAnimator2>()
    private var volumeBars: CurveAnimatorBars? = null
    private var rsiCurve: CurveAnimator2? = null

    init {
        setExtremum()

        mainCurve = CurveAnimator2(
            target.valuesByTimestamp(),
            minKey,
            maxKey,
            minValue,
            maxValue
        )

        initMovingAverages()

        initRsiCurve()

        if (hasVolumes) {
            val volumeByTimestamp = target.volumeByTimestamp()
            val volumeMin = volumeByTimestamp.minOf { it.value }
            val volumeMax = volumeByTimestamp.maxOf { it.value }
            volumeBars = CurveAnimatorBars(
                volumeByTimestamp,
                minKey,
                maxKey,
                volumeMin,
                volumeMax
            )
        }
    }

    private fun initRsiCurve() {
        rsiCurve = target.rsi?.let { chartIndicatorRsi ->
            val values = chartIndicatorRsi.points
            CurveAnimator2(
                values,
                minKey,
                maxKey,
                values.minOf { it.value },
                values.maxOf { it.value }
            )
        }
    }

    private fun initMovingAverages() {
        movingAverageCurves.clear()
        movingAverageCurves.putAll(
            target.movingAverages
                .map { (id, movingAverage: ChartIndicator) ->
                    id to CurveAnimator2(
                        movingAverage.line,
                        minKey,
                        maxKey,
                        minValue,
                        maxValue
                    ).apply {
                        color = movingAverage.color
                    }
                }
        )
    }

    fun getMainCurveState(): CurveAnimator2.UiState {
        return mainCurve.state
    }

    fun getVolumeBarsState(): CurveAnimatorBars.UiState? {
        return volumeBars?.state
    }

    fun getMovingAverageCurveStates(): List<CurveAnimator2.UiState> {
        return movingAverageCurves.map { it.value.state }
    }

    fun getRsiCurveState(): CurveAnimator2.UiState? {
        return rsiCurve?.state
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

        if (target.movingAverages.keys != movingAverageCurves.keys) {
            initMovingAverages()
        } else {
            target.movingAverages.forEach { (id, u: ChartIndicator) ->
                movingAverageCurves[id]?.setTo(
                    u.line,
                    minKey,
                    maxKey,
                    minValue,
                    maxValue,
                )
            }
        }

        if (target.rsi == null) {
            rsiCurve = null
        } else if (rsiCurve == null) {
            initRsiCurve()
        } else {
            target.rsi?.let { chartIndicatorRsi ->
                val values = chartIndicatorRsi.points
                rsiCurve?.setTo(
                    values,
                    minKey,
                    maxKey,
                    values.minOf { it.value },
                    values.maxOf { it.value }
                )
            }
        }

        if (hasVolumes) {
            val volumeByTimestamp = target.volumeByTimestamp()
            val volumeMin = volumeByTimestamp.minOf { it.value }
            val volumeMax = volumeByTimestamp.maxOf { it.value }
            volumeBars?.setValues(
                volumeByTimestamp,
                minKey,
                maxKey,
                volumeMin,
                volumeMax
            )
        }
    }

    fun onNextFrame(value: Float) {
        mainCurve.onNextFrame(value)
        movingAverageCurves.forEach { (_, curve) ->
            curve.onNextFrame(value)
        }
        volumeBars?.onNextFrame(value)
        rsiCurve?.onNextFrame(value)
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

        val nearestPercentagePositionX = (nearestChartPoint.timestamp - minKey) / interval.toFloat()

        selectedItem = SelectedItem(
            percentagePositionX = nearestPercentagePositionX,
            chartPoint = nearestChartPoint
        )
    }

}

data class SelectedItem(val percentagePositionX: Float, val chartPoint: ChartPoint)
