package io.horizontalsystems.chartview

import android.util.Range
import androidx.compose.runtime.Immutable
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.chartview.models.ChartPointF
import java.math.BigDecimal
import kotlin.math.abs

@Immutable
data class ChartData(
    val items: List<ChartDataItemImmutable>,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isExpired: Boolean,
    val valueRange: Range<Float>,
    val isMovementChart: Boolean
) {

    fun values(name: Indicator): List<ChartDataValueImmutable> {
        return items.mapNotNull { it.values[name] }
    }

    fun valuesByTimestamp(name: Indicator): LinkedHashMap<Long, Float> {
        return LinkedHashMap(
            items.mapNotNull { item ->
                item.values[name]?.let {
                    item.timestamp to it.value
                }
            }.toMap()
        )
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

        return try {
            ((lastValue - firstValue) / firstValue * 100).toBigDecimal()
        } catch(e: Exception) {
            BigDecimal.ZERO
        }
    }

    fun sum(): BigDecimal {
        val values = items.mapNotNull { it.values[Indicator.Candle]?.value }

        return values.sum().toBigDecimal()
    }
}

@Immutable
data class ChartDataValueImmutable(val value: Float, val point: ChartPointF)

@Immutable
data class ChartDataItemImmutable(
    val timestamp: Long,
    val values: Map<Indicator, ChartDataValueImmutable?>
)

class ChartDataBuilder private constructor(
    points: List<ChartPoint>,
    start: Long?,
    end: Long?,
    private val isExpired: Boolean = false,
    private val isMovementChart: Boolean
) {

    companion object {
        val placeholder = ChartDataBuilder(
            listOf(
                ChartPoint(2.toFloat(), 100, mapOf()),
                ChartPoint(2.toFloat(), 200, mapOf()),
                ChartPoint(1.toFloat(), 300, mapOf()),
                ChartPoint(3.toFloat(), 400, mapOf()),
                ChartPoint(2.toFloat(), 500, mapOf()),
                ChartPoint(2.toFloat(), 600, mapOf())
            ),
            100,
            600,
            true,
            false
        ).build()

        fun buildFromPoints(
            points: List<ChartPoint>,
            startTimestamp: Long? = null,
            endTimestamp: Long? = null,
            isExpired: Boolean = false,
            isMovementChart: Boolean = true
        ): ChartData {
            return ChartDataBuilder(
                points,
                startTimestamp,
                endTimestamp,
                isExpired,
                isMovementChart
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

        val max = listOf(
            macd?.lower,
            macd?.upper,
            signal?.lower,
            signal?.upper,
            histogram?.lower,
            histogram?.upper
        )
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

            val valuesImmutable = mapOf(Indicator.Candle to ChartDataValueImmutable(point.value, ChartPointF(x, getPointY(point.value, getRangeForIndicator(Indicator.Candle)))))
                .plus(
                    point.indicators.mapNotNull { (indicator, value) ->
                        value?.let {
                            indicator to ChartDataValueImmutable(value, ChartPointF(x, getPointY(value, getRangeForIndicator(indicator))))
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
        return ChartData(immutableItems, startTimestamp, endTimestamp, isExpired, valueRange, isMovementChart)
    }
}
