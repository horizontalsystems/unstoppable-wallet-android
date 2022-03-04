package io.horizontalsystems.chartview

import android.util.Log
import io.horizontalsystems.chartview.models.ChartPointF

class Zzz(
    data: ChartData,
    private val fromValues: LinkedHashMap<Long, Float>,
    private val fromStartTimestamp: Long,
    private val fromEndTimestamp: Long,
    private val fromMinValue: Float,
    private val fromMaxValue: Float,

    private val xMax: Float,
    private val yMax: Float,
    private val curveVerticalOffset: Float,

    ) {
    private val toValues: LinkedHashMap<Long, Float>
    private val toStartTimestamp = data.startTimestamp
    private val toEndTimestamp = data.endTimestamp

    private val toMinValue: Float
    private val toMaxValue: Float

    var frameValues = fromValues
        private set
    var frameStartTimestamp = fromStartTimestamp
        private set
    var frameEndTimestamp = fromEndTimestamp
        private set
    var frameMinValue = fromMinValue
        private set
    var frameMaxValue = fromMaxValue
        private set

    private val fromValuesFilled: LinkedHashMap<Long, Float>
    private val toValuesFilled: LinkedHashMap<Long, Float>

    init {
        toValues = LinkedHashMap(
            data.items.mapNotNull { chartDataItem ->
                chartDataItem.values[Indicator.Candle]?.let {
                    chartDataItem.timestamp to it.value
                }
            }.toMap().toSortedMap()
        )
        Log.e("AAA", "fromValues: $fromValues")
//        Log.e("AAA", "toValues: $toValues")

        toMinValue = toValues.values.minOrNull() ?: 0f
        toMaxValue = toValues.values.maxOrNull() ?: 0f

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
                if (timestamp < frameStartTimestamp || timestamp > frameEndTimestamp) continue

                val valueTo = toValuesFilled[timestamp]!!

                val valueForFrame = getForFrame(valueFrom, valueTo, animatedFraction)
                if (valueForFrame < frameMinValue || valueForFrame > frameMaxValue) continue

                values[timestamp] = valueForFrame
            }

            frameValues = LinkedHashMap(values)
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

            val prevTimestamps = prevPointsMap.keys

            for ((timestamp, value) in nextPointsMap) {
                if (!prevPointsMutableMap.containsKey(timestamp)) {
                    val timeStampBefore = prevTimestamps.lastOrNull { it < timestamp } ?: continue
                    val timeStampAfter = prevTimestamps.firstOrNull { it > timestamp } ?: continue

                    val valueBefore = prevPointsMutableMap[timeStampBefore]!!
                    val valueAfter = prevPointsMutableMap[timeStampAfter]!!

                    // v = (t - t1) * (v2 - v1) / (t2 - t1) + v1

                    val t1 = timeStampBefore
                    val t2 = timeStampAfter
                    val v1 = valueBefore
                    val v2 = valueAfter
                    val t = timestamp

                    val v = (t - t1) * (v2 - v1) / (t2 - t1) + v1

                    prevPointsMutableMap[timestamp] = v
                }
            }

            for ((timestamp, value) in nextPointsMap) {
                if (!prevPointsMutableMap.containsKey(timestamp)) {
                    prevPointsMutableMap[timestamp] = value
                }
            }

            return LinkedHashMap(prevPointsMutableMap.toSortedMap())
        }

    }

}
