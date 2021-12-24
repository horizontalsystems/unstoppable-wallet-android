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

data class ChartDataValue(val value: Float) {
    var point: PointF = PointF(0f, 0f)

    fun toImmutable(): ChartDataValueImmutable {
        return ChartDataValueImmutable(value, point)
    }
}

data class ChartDataItem(val timestamp: Long, val values: MutableMap<Indicator, ChartDataValue?> = mutableMapOf()) {

    fun setPoint(x: Float, indicator: Indicator, range: Range<Float>) {
        val delta = range.upper - range.lower
        values[indicator]?.let {
            val y = if (delta == 0F && range.upper > 0f) 0.5f else (it.value - range.lower) / delta
            it.point = PointF(x, y)
        }
    }

    fun toImmutable(): ChartDataItemImmutable {
        return ChartDataItemImmutable(timestamp, values.map {
            it.key to it.value?.toImmutable()
        }.toMap())
    }
}

class ChartDataBuilder(val items: MutableList<ChartDataItem>, val startTimestamp: Long, val endTimestamp: Long, private val isExpired: Boolean = false) {

    companion object {
        fun buildFromPoints(points: List<ChartPoint>): ChartData {
            val items = points.map { point ->
                ChartDataItem(point.timestamp, mutableMapOf(Indicator.Candle to ChartDataValue(point.value)))
            }
            return ChartDataBuilder(items.toMutableList(), points.first().timestamp, points.last().timestamp, false).build()
        }
    }
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

    init {
        items.forEach {
            it.values.forEach { (indicator, chartDataValue) ->
                range(it, indicator)
            }
        }

        val visibleTimeInterval = endTimestamp - startTimestamp

        items.forEach { item ->
            val timestamp = item.timestamp - startTimestamp
            val x = (timestamp.toFloat() / visibleTimeInterval)
            item.values.forEach { (indicator, chartDataValue) ->
                item.setPoint(x, indicator, getRangeForIndicator(indicator))
            }
        }
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

    fun add(values: List<ChartDataValue?>, name: Indicator) {
        val start = items.size - values.size
        for (i in values.indices) {
            val value = values[i] ?: continue
            items[i + start].values[name] = value
        }
    }

    // Ranges

    fun range(item: ChartDataItem, indicator: Indicator) {
        val currValue = item.values[indicator]?.value ?: return
        val prevRange = ranges[indicator] ?: Range(currValue, currValue)

        var prevLower = prevRange.lower
        var prevUpper = prevRange.upper

        if (prevLower > currValue) {
            prevLower = currValue
        }

        if (prevUpper < currValue) {
            prevUpper = currValue
        }

        ranges[indicator] = Range(prevLower, prevUpper)
    }

    fun build(): ChartData {
        return ChartData(items.map { it.toImmutable() }, startTimestamp, endTimestamp, isExpired, valueRange)
    }
}
