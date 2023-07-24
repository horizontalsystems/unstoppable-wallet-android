package io.horizontalsystems.chartview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class CurveAnimatorBars(
    private var targetValues: LinkedHashMap<Long, Float>,
    private var targetMinKey: Long,
    private var targetMaxKey: Long,
    private var targetMinValue: Float,
    private var targetMaxValue: Float,
) {
    private var fromValues: LinkedHashMap<Long, Float> = targetValues
    private var fromMinKey: Long = targetMinKey
    private var fromMaxKey: Long = targetMaxKey
    private var fromMinValue: Float = targetMinValue
    private var fromMaxValue: Float = targetMaxValue

    private var frameValues: LinkedHashMap<Long, Float> = fromValues
    private var frameMinKey: Long = fromMinKey
    private var frameMaxKey: Long = fromMaxKey
    private var frameMinValue: Float = fromMinValue
    private var frameMaxValue: Float = fromMaxValue

    private var combinedKeys = (fromValues.keys + targetValues.keys).distinct().sorted()

    data class UiState(
        val values: LinkedHashMap<Long, Float>,
        val startTimestamp: Long,
        val endTimestamp: Long,
        val minValue: Float,
        val maxValue: Float,
    )

    var state by mutableStateOf(
        UiState(
            values = frameValues,
            startTimestamp = frameMinKey,
            endTimestamp = frameMaxKey,
            minValue = frameMinValue,
            maxValue = frameMaxValue,
        )
    )
        private set

    fun setValues(
        values: LinkedHashMap<Long, Float>,
        minKey: Long,
        maxKey: Long,
        minValue: Float = values.minOfOrNull { it.value } ?: 0f,
        maxValue: Float = values.maxOfOrNull { it.value } ?: 0f,
    ) {
        targetValues = values
        targetMinKey = minKey
        targetMaxKey = maxKey
        targetMinValue = minValue
        targetMaxValue = maxValue

        fromValues = frameValues
        fromMinKey = frameMinKey
        fromMaxKey = frameMaxKey
        fromMinValue = frameMinValue
        fromMaxValue = frameMaxValue

        if (targetMinValue == targetMaxValue) {
            targetMinValue *= 0.9f
        }

        if (minKey == maxKey) {
            targetMinKey = (minKey * 0.9).toLong()
            targetMaxKey = (maxKey * 1.1).toLong()

            // turn off animation
            fromValues = targetValues
            fromMinKey = targetMinKey
            fromMaxKey = targetMaxKey
            fromMinValue = targetMinValue
            fromMaxValue = targetMaxValue
        }

        combinedKeys = (fromValues.keys + targetValues.keys).distinct().sorted()
    }

    fun onNextFrame(animatedFraction: Float) {
        if (fromValues.isEmpty() || animatedFraction == 1f) {
            frameValues = targetValues
            frameMinKey = targetMinKey
            frameMaxKey = targetMaxKey
            frameMinValue = targetMinValue
            frameMaxValue = targetMaxValue
        } else {
            frameValues = LinkedHashMap(
                combinedKeys.map { key ->
                    val fromValue = fromValues.getOrDefault(key, fromMinValue / 10)
                    val targetValue = targetValues.getOrDefault(key, targetMinValue / 10)
                    val frameValue = changeByPercentage(fromValue, targetValue, animatedFraction)

                    key to frameValue
                }.toMap()
            )
            frameMinKey = changeByPercentage(fromMinKey, targetMinKey, animatedFraction)
            frameMaxKey = changeByPercentage(fromMaxKey, targetMaxKey, animatedFraction)
            frameMinValue = changeByPercentage(fromMinValue, targetMinValue, animatedFraction)
            frameMaxValue = changeByPercentage(fromMaxValue, targetMaxValue, animatedFraction)
        }

        emitState()
    }

    private fun emitState() {
        state = UiState(
            values = frameValues,
            startTimestamp = frameMinKey,
            endTimestamp = frameMaxKey,
            minValue = frameMinValue,
            maxValue = frameMaxValue,
        )
    }

    private fun changeByPercentage(fromValue: Float, targetValue: Float, percentage: Float): Float {
        val change = targetValue - fromValue
        return fromValue + change * percentage
    }

    private fun changeByPercentage(fromValue: Long, targetValue: Long, percentage: Float): Long {
        val change = targetValue - fromValue
        return (fromValue + change * percentage).toLong()
    }
}
