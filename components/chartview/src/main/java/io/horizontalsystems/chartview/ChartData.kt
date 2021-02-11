package io.horizontalsystems.chartview

import android.graphics.PointF
import android.util.Range
import java.math.BigDecimal
import kotlin.math.abs

class ChartData(val items: MutableList<Item>, val startTimestamp: Long, val endTimestamp: Long, val isExpired: Boolean = false) {

    class Value(val value: Float, var point: PointF = PointF(0f, 0f))

    class Item(val timestamp: Long, val values: MutableMap<Indicator, Value?> = mutableMapOf()) {

        fun setPoint(x: Float, indicator: Indicator, range: Range<Float>) {
            values[indicator]?.let {
                val y = (it.value - range.lower) / (range.upper - range.lower)
                it.point = PointF(x, y)
            }
        }
    }

    fun values(name: Indicator): List<Value> {
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

    fun add(values: List<Value?>, name: Indicator) {
        val start = items.size - values.size
        for (i in values.indices) {
            val value = values[i] ?: continue
            items[i + start].values[name] = value
        }
    }

    fun insert(item: Item) {
        val index = items.indexOfFirst { it.timestamp >= item.timestamp }
        if (index == -1) {
            return
        }

        if (items[index].timestamp == item.timestamp) {
            items.removeAt(index)
        }

        items.add(index, item)
    }

    // Ranges

    private val ranges: MutableMap<Indicator, Range<Float>> = mutableMapOf()

    fun range(item: Item, indicator: Indicator) {
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

    val valueRange by lazy {
        ranges[Indicator.Candle] ?: Range(0f, 1f)
    }

    val volumeRange by lazy {
        val range = ranges[Indicator.Volume]
        Range(0f, range?.upper ?: 1f)
    }

    val rsiRange = Range(0f, 100f)

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
}
