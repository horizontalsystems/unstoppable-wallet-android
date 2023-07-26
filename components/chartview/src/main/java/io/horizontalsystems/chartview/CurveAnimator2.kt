package io.horizontalsystems.chartview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class CurveAnimator2(
    private var toValues: LinkedHashMap<Long, Float>,
    private var toStartTimestamp: Long,
    private var toEndTimestamp: Long,
    private var toMinValue: Float,
    private var toMaxValue: Float,
) {
    var color: Long? = null

    private var fromValues: LinkedHashMap<Long, Float> = toValues
    private var fromStartTimestamp: Long = toStartTimestamp
    private var fromEndTimestamp: Long = toEndTimestamp
    private var fromMinValue: Float = toMinValue
    private var fromMaxValue: Float = toMaxValue

    private var frameValues: LinkedHashMap<Long, Float> = fromValues
    private var frameStartTimestamp: Long = fromStartTimestamp
    private var frameEndTimestamp: Long = fromEndTimestamp
    private var frameMinValue: Float = fromMinValue
    private var frameMaxValue: Float = fromMaxValue

    private var fromValuesFilled = fillWith(fromValues, toValues)
    private var toValuesFilled = fillWith(toValues, fromValues)

    data class UiState(
        val values: LinkedHashMap<Long, Float>,
        val startTimestamp: Long,
        val endTimestamp: Long,
        val minValue: Float,
        val maxValue: Float,
        val color: Long?
    )

    var state by mutableStateOf(
        UiState(
            values = frameValues,
            startTimestamp = frameStartTimestamp,
            endTimestamp = frameEndTimestamp,
            minValue = frameMinValue,
            maxValue = frameMaxValue,
            color = color,
        )
    )
        private set


    fun setTo(
        toValues: LinkedHashMap<Long, Float>,
        toStartTimestamp: Long,
        toEndTimestamp: Long,
        toMinValue: Float,
        toMaxValue: Float,
    ) {
        fromValues = frameValues
        fromStartTimestamp = frameStartTimestamp
        fromEndTimestamp = frameEndTimestamp
        fromMinValue = frameMinValue
        fromMaxValue = frameMaxValue

        this.toValues = toValues
        this.toStartTimestamp = toStartTimestamp
        this.toEndTimestamp = toEndTimestamp
        this.toMinValue = toMinValue
        this.toMaxValue = toMaxValue

        fromValuesFilled = fillWith(fromValues, this.toValues)
        toValuesFilled = fillWith(this.toValues, fromValues)
    }

    fun onNextFrame(animatedFraction: Float) {
        if (fromValues.isEmpty() || animatedFraction == 1f) {
            frameStartTimestamp = toStartTimestamp
            frameEndTimestamp = toEndTimestamp

            frameMinValue = toMinValue
            frameMaxValue = toMaxValue

            frameValues = toValues
        } else {
            frameStartTimestamp = getForFrame(fromStartTimestamp.toFloat(), toStartTimestamp.toFloat(), animatedFraction).toLong()
            frameEndTimestamp = getForFrame(fromEndTimestamp.toFloat(), toEndTimestamp.toFloat(), animatedFraction).toLong()

            frameMinValue = getForFrame(fromMinValue, toMinValue, animatedFraction)
            frameMaxValue = getForFrame(fromMaxValue, toMaxValue, animatedFraction)

            val values = mutableMapOf<Long, Float>()
            for ((timestamp, valueFrom) in fromValuesFilled) {
                val valueTo = toValuesFilled[timestamp]!!
                values[timestamp] = getForFrame(valueFrom, valueTo, animatedFraction)
            }

            frameValues = LinkedHashMap(values.toSortedMap())
        }

        emitState()
    }

    private fun emitState() {
        state = UiState(
            values = frameValues,
            startTimestamp = frameStartTimestamp,
            endTimestamp = frameEndTimestamp,
            minValue = frameMinValue,
            maxValue = frameMaxValue,
            color = color,
        )
    }

    private fun getForFrame(start: Float, end: Float, animatedFraction: Float): Float {
        val change = end - start

        return start + (change * animatedFraction)
    }

    companion object {

        fun fillWith(
            prevPointsMap: LinkedHashMap<Long, Float>,
            nextPointsMap: LinkedHashMap<Long, Float>,
        ): LinkedHashMap<Long, Float> {
            val prevPointsMutableMap = prevPointsMap.toMutableMap()

            val prevTimestamps = prevPointsMutableMap.keys.toList()

            for ((timestamp, _) in nextPointsMap) {
                if (!prevPointsMutableMap.containsKey(timestamp)) {
                    valueForTimestamp(timestamp, prevTimestamps, prevPointsMutableMap)?.let {
                        prevPointsMutableMap[timestamp] = it
                    }
                }
            }

            for ((timestamp, value) in nextPointsMap) {
                if (!prevPointsMutableMap.containsKey(timestamp)) {
                    prevPointsMutableMap[timestamp] = value
                }
            }

            return LinkedHashMap(prevPointsMutableMap.toSortedMap())
        }

        private fun valueForTimestamp(
            timestamp: Long,
            timestamps: Collection<Long>,
            values: Map<Long, Float>
        ): Float? {
            val timeStampBefore = timestamps.lastOrNull { it < timestamp } ?: return null
            val timeStampAfter = timestamps.firstOrNull { it > timestamp } ?: return null

            val valueBefore = values[timeStampBefore] ?: return null
            val valueAfter = values[timeStampAfter] ?: return null

            // v = (t - t1) * (v2 - v1) / (t2 - t1) + v1

            val t1 = timeStampBefore
            val t2 = timeStampAfter
            val v1 = valueBefore
            val v2 = valueAfter
            val t = timestamp

            return (t - t1) * (v2 - v1) / (t2 - t1) + v1
        }
    }
}