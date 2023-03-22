package io.horizontalsystems.chartview

import android.util.Range
import androidx.compose.runtime.Immutable
import io.horizontalsystems.chartview.models.ChartPoint
import java.math.BigDecimal

@Immutable
data class ChartData(
    val items: List<ChartDataItemImmutable>,
    val valueRange: Range<Float>,
    val isMovementChart: Boolean,
    val disabled: Boolean = false
) {
    val startTimestamp: Long by lazy {
        items.first().timestamp
    }

    val endTimestamp: Long by lazy {
        items.last().timestamp
    }

    fun values(name: Indicator): List<Float> {
        return items.mapNotNull { it.values[name] }
    }

    fun valuesByTimestamp(name: Indicator): LinkedHashMap<Long, Float> {
        return LinkedHashMap(
            items.mapNotNull { item ->
                item.values[name]?.let {
                    item.timestamp to it
                }
            }.toMap()
        )
    }

    fun diff(): BigDecimal {
        val values = items.mapNotNull { it.values[Indicator.Candle] }
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
        val values = items.mapNotNull { it.values[Indicator.Candle] }

        return values.sum().toBigDecimal()
    }
}

@Immutable
data class ChartDataItemImmutable(
    val timestamp: Long,
    val values: Map<Indicator, Float?>
)

class ChartDataBuilder constructor(
    points: List<ChartPoint>,
    private val isMovementChart: Boolean,
    private val disabled: Boolean = false
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
            true
        ).build()

        fun buildFromPoints(
            points: List<ChartPoint>,
            isMovementChart: Boolean = true,
            isDisabled: Boolean = false,
        ): ChartData {
            return ChartDataBuilder(
                points,
                isMovementChart,
                isDisabled
            ).build()
        }
    }

    private val ranges: MutableMap<Indicator, Range<Float>> = mutableMapOf()

    val valueRange by lazy {
        ranges[Indicator.Candle] ?: Range(0f, 1f)
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

        immutableItems = points.map { point ->
            val valuesImmutable = mapOf(Indicator.Candle to point.value)
                .plus(
                    point.indicators.mapNotNull { (indicator, value) ->
                        value?.let {
                            indicator to value
                        }
                    }
                )

            ChartDataItemImmutable(point.timestamp, valuesImmutable)
        }
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
        return ChartData(immutableItems, valueRange, isMovementChart, disabled)
    }
}
