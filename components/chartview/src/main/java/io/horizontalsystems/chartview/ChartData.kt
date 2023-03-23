package io.horizontalsystems.chartview

import android.util.Range
import androidx.compose.runtime.Immutable
import io.horizontalsystems.chartview.models.ChartPoint
import java.math.BigDecimal

@Immutable
data class ChartData(
    val items: List<ChartPoint>,
    val isMovementChart: Boolean,
    val disabled: Boolean = false
) {
    val valueRange: Range<Float> by lazy {
        Range(items.minOf { it.value }, items.maxOf { it.value })
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
