package io.horizontalsystems.chartview

import io.horizontalsystems.chartview.models.ChartPointF

class Zzz(
    private val toValues: LinkedHashMap<Long, Float>,
    private val toStartTimestamp: Long,
    private val toEndTimestamp: Long,
    private val toMinValue: Float,
    private val toMaxValue: Float,
    prevZzz: Zzz?,
    private val xMax: Float,
    private val yMax: Float,
    private val curveVerticalOffset: Float,
) {
    private val fromValues: LinkedHashMap<Long, Float>
    private val fromStartTimestamp: Long
    private val fromEndTimestamp: Long
    private val fromMinValue: Float
    private val fromMaxValue: Float

    var frameValues: LinkedHashMap<Long, Float>
        private set
    var frameStartTimestamp: Long
        private set
    var frameEndTimestamp: Long
        private set
    var frameMinValue: Float
        private set
    var frameMaxValue: Float
        private set

    private val fromValuesFilled: LinkedHashMap<Long, Float>
    private val toValuesFilled: LinkedHashMap<Long, Float>

    init {
        if (prevZzz != null) {
            fromValues = prevZzz.frameValues
            fromStartTimestamp = prevZzz.frameStartTimestamp
            fromEndTimestamp = prevZzz.frameEndTimestamp
            fromMinValue = prevZzz.frameMinValue
            fromMaxValue = prevZzz.frameMaxValue
        } else {
            fromValues = LinkedHashMap(
                toValues.map { (timestamp, _) ->
                    timestamp to 0F
                }.toMap()
            )
            fromStartTimestamp = toStartTimestamp
            fromEndTimestamp = toEndTimestamp
            fromMinValue = toMinValue
            fromMaxValue = toMinValue
        }

        frameValues = fromValues
        frameStartTimestamp = fromStartTimestamp
        frameEndTimestamp = fromEndTimestamp
        frameMinValue = fromMinValue
        frameMaxValue = fromMaxValue

        fromValuesFilled = fillWith(fromValues, toValues)
        toValuesFilled = fillWith(toValues, fromValues)
    }

    fun nextFrame(animatedFraction: Float) {
        if (fromValues.isEmpty()) {
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
    }

    private fun getForFrame(start: Float, end: Float, animatedFraction: Float): Float {
        val change = end - start

        return start + (change * animatedFraction)
    }

    fun getFramePoints(): List<ChartPointF> {
        // timestamp = ax + startTimestamp
        // x = (timestamp - startTimestamp) / a
        // a = (timestamp - startTimestamp) / x
        val xRatio = (frameEndTimestamp - frameStartTimestamp) / xMax

        // value = ay + minValue
        // y = (value - minValue) / a
        // a = (value - minValue) / y
        val yRatio = (frameMaxValue - frameMinValue) / (yMax - 2 * curveVerticalOffset)

        return frameValues.map { (timestamp, value) ->
            val x = (timestamp - frameStartTimestamp) / xRatio
            val y = (value - frameMinValue) / yRatio + curveVerticalOffset

            val y2 = (y * -1) + yMax

            ChartPointF(x, y2)
        }
    }


    companion object {
        fun fillWith(
            prevPointsMap: LinkedHashMap<Long, Float>,
            nextPointsMap: LinkedHashMap<Long, Float>,
        ): LinkedHashMap<Long, Float> {
            val prevPointsMutableMap = prevPointsMap.toMutableMap()

            val prevTimestamps = prevPointsMutableMap.keys

            for ((timestamp, value) in nextPointsMap) {
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
