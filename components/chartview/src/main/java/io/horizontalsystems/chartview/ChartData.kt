package io.horizontalsystems.chartview

import androidx.compose.runtime.Immutable
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.ChartPoint
import java.lang.Float.max
import java.lang.Float.min
import java.math.BigDecimal

@Immutable
data class ChartData(
    val items: List<ChartPoint>,
    val isMovementChart: Boolean,
    val disabled: Boolean = false,
    val indicators: Map<String, ChartIndicator> = mapOf(),
) {
    var macd: ChartIndicator.Macd? = null
    var rsi: ChartIndicator.Rsi? = null

    val movingAverages by lazy {
        buildMap {
            indicators.forEach { (id, indicator) ->
                if (indicator is ChartIndicator.MovingAverage) {
                    put(id, indicator)
                }
            }
        }
    }

    init {
        for (indicator in indicators.values) {
            when (indicator) {
                is ChartIndicator.Macd -> {
                    macd = indicator
                }

                is ChartIndicator.Rsi -> {
                    rsi = indicator
                }

                is ChartIndicator.MovingAverage -> {

                }
            }
        }
    }

    val minValue: Float by lazy {
        var valuesMin = items.minOf { it.value }
        movingAverages
            .mapNotNull { it.value.line.minOfOrNull { it.value } }
            .minOrNull()
            ?.let { indicatorsMin ->
                valuesMin = min(valuesMin, indicatorsMin)
            }

        valuesMin
    }

    val maxValue: Float by lazy {
        var valuesMax = items.maxOf { it.value }
        movingAverages
            .mapNotNull { it.value.line.maxOfOrNull { it.value } }
            .maxOrNull()
            ?.let { indicatorsMax ->
                valuesMax = max(valuesMax, indicatorsMax)
            }
        valuesMax
    }

    val startTimestamp: Long by lazy {
        items.first().timestamp
    }

    val endTimestamp: Long by lazy {
        items.last().timestamp
    }

    fun valuesByTimestamp(): LinkedHashMap<Long, Float> {
        return LinkedHashMap(
            items.associate { item ->
                item.timestamp to item.value
            }
        )
    }

    fun volumeByTimestamp(): LinkedHashMap<Long, Float> {
        return LinkedHashMap(
            items.mapNotNull { item ->
                item.volume?.let {
                    item.timestamp to it
                }
            }.toMap()
        )
    }

    fun dominanceByTimestamp(): LinkedHashMap<Long, Float> {
        return LinkedHashMap(
            items.mapNotNull { item ->
                item.dominance?.let {
                    item.timestamp to it
                }
            }.toMap()
        )
    }

    fun diff(): BigDecimal {
        val values = items.map { it.value }
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
        return items.map { it.value }.sum().toBigDecimal()
    }
}
