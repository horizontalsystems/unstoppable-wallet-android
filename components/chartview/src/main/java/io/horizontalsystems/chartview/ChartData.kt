package io.horizontalsystems.chartview

import android.graphics.PointF
import android.util.Range
import androidx.compose.runtime.Immutable
import io.horizontalsystems.chartview.models.ChartPoint
import java.math.BigDecimal
import kotlin.math.abs

@Immutable
data class ChartData(
    val items: List<ChartDataItemImmutable>,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isExpired: Boolean = false,
    val valueRange: Range<Float>
) {

    fun values(name: Indicator): List<ChartDataValueImmutable> {
        return items.mapNotNull { it.values[name] }
    }

    fun diff(): BigDecimal {
        val values = items.mapNotNull { it.values[Indicator.Candle]?.value }
        if (values.isEmpty()) {
            return BigDecimal.ZERO
        }

        val firstValue = values.find { it != 0f }
        val lastValue = values.last()
        if (lastValue == 0f || firstValue == null) {
            return BigDecimal.ZERO
        }

        return ((lastValue - firstValue) / firstValue * 100).toBigDecimal()
    }
}

@Immutable
data class ChartDataValueImmutable(val value: Float, val point: PointF)

@Immutable
data class ChartDataItemImmutable(val timestamp: Long, val values: Map<Indicator, ChartDataValueImmutable?>)

class ChartDataBuilder private constructor(points: List<ChartPoint>, start: Long?, end: Long?, private val isExpired: Boolean = false) {

    companion object {
        fun buildFromPoints(
            points: List<ChartPoint>,
            startTimestamp: Long? = null,
            endTimestamp: Long? = null,
            isExpired: Boolean = false
        ): ChartData {
            return ChartDataBuilder(
                points,
                startTimestamp,
                endTimestamp,
                isExpired
            ).build()
        }
    }

    private val startTimestamp = start ?: points.first().timestamp
    private val endTimestamp = end ?: points.last().timestamp

    private val ranges: MutableMap<Indicator, Range<Float>> = mutableMapOf()

    val valueRange by lazy {
        ranges[Indicator.Candle] ?: Range(0f, 1f)
    }

    val volumeRange by lazy {
        val range = ranges[Indicator.Volume]
        Range(0f, range?.upper ?: 1f)
    }

    val rsiRange = Range(0f, 100f)

    val dominanceRange by lazy {
        ranges[Indicator.Dominance] ?: Range(0f, 1f)
    }

    val histogramRange by lazy {
        val histogram = ranges[Indicator.MacdHistogram]

        val max = listOf(histogram?.lower, histogram?.upper)
                .mapNotNull { it }
                .map { abs(it) }
                .maxOrNull() ?: 1f

        Range(-max, max)
    }

    val macdRange by lazy {
        val macd = ranges[Indicator.Macd]
        val signal = ranges[Indicator.MacdSignal]
        val histogram = ranges[Indicator.MacdHistogram]

        val max = listOf(macd?.lower, macd?.upper, signal?.lower, signal?.upper, histogram?.lower, histogram?.upper)
                .mapNotNull { it }
                .map { abs(it) }
                .maxOrNull() ?: 1f

        Range(-max, max)
    }

    val immutableItems: List<ChartDataItemImmutable>

    init {
        points.forEach { point: ChartPoint ->
            adjustRange(Indicator.Candle, point.value)
            point.indicators.forEach { (indicator, value) ->
                value?.let {
                    adjustRange(indicator, it)
                }
            }
        }

        val visibleTimeInterval = endTimestamp - startTimestamp

        immutableItems = points.map { point ->
            val timestamp = point.timestamp - startTimestamp
            val x = (timestamp.toFloat() / visibleTimeInterval)

            val valuesImmutable = mapOf(Indicator.Candle to ChartDataValueImmutable(point.value, PointF(x, getPointY(point.value, getRangeForIndicator(Indicator.Candle)))))
                .plus(
                    point.indicators.mapNotNull { (indicator, value) ->
                        value?.let {
                            indicator to ChartDataValueImmutable(value, PointF(x, getPointY(value, getRangeForIndicator(indicator))))
                        }
                    }
                )

            ChartDataItemImmutable(point.timestamp, valuesImmutable)
        }
    }

    private fun getPointY(
        value: Float,
        range: Range<Float>,
    ): Float {
        val delta = range.upper - range.lower
        return if (delta == 0F && range.upper > 0f) 0.5f else (value - range.lower) / delta
    }


    fun getRangeForIndicator(indicator: Indicator) = when (indicator) {
        Indicator.Candle -> valueRange
        Indicator.EmaFast -> valueRange
        Indicator.EmaSlow -> valueRange
        Indicator.Volume -> volumeRange
        Indicator.Dominance -> dominanceRange
        Indicator.Rsi -> rsiRange
        Indicator.Macd -> macdRange
        Indicator.MacdSignal -> macdRange
        Indicator.MacdHistogram -> histogramRange
    }

    // Ranges

    private fun adjustRange(indicator: Indicator, value: Float) {
        val prevRange = ranges[indicator] ?: Range(value, value)

        var prevLower = prevRange.lower
        var prevUpper = prevRange.upper

        if (prevLower > value) {
            prevLower = value
        }

        if (prevUpper < value) {
            prevUpper = value
        }

        ranges[indicator] = Range(prevLower, prevUpper)
    }

    fun build(): ChartData {
        return ChartData(immutableItems, startTimestamp, endTimestamp, isExpired, valueRange)
    }
}
