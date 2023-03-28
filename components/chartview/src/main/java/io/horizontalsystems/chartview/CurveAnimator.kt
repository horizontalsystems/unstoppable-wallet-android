package io.horizontalsystems.chartview

import io.horizontalsystems.chartview.models.ChartPointF

class CurveAnimator(
    private val toValues: LinkedHashMap<Long, Float>,
    private var toStartTimestamp: Long,
    private var toEndTimestamp: Long,
    private var toMinValue: Float,
    private var toMaxValue: Float,
    prevCurveAnimator: CurveAnimator?,
    private val xMax: Float,
    private val yMax: Float,
    private val curveTopOffset: Float,
    private val curveBottomOffset: Float,
    private val horizontalOffset: Float,
) {
    private var fromValues: LinkedHashMap<Long, Float>
    private var fromStartTimestamp: Long
    private var fromEndTimestamp: Long
    private var fromMinValue: Float
    private var fromMaxValue: Float

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
//    private val matchTimestamps: List<Pair<Long, Long>>

    init {
        if (prevCurveAnimator != null) {
            fromValues = prevCurveAnimator.frameValues
            fromStartTimestamp = prevCurveAnimator.frameStartTimestamp
            fromEndTimestamp = prevCurveAnimator.frameEndTimestamp
            fromMinValue = prevCurveAnimator.frameMinValue
            fromMaxValue = prevCurveAnimator.frameMaxValue
        } else {
            fromValues = toValues
            fromStartTimestamp = toStartTimestamp
            fromEndTimestamp = toEndTimestamp
            fromMinValue = toMinValue
            fromMaxValue = toMaxValue
        }

        if (toMinValue == toMaxValue) {
            toMinValue *= 0.9f
            toMaxValue *= 1.1f
        }

        if (toStartTimestamp == toEndTimestamp) {
            toStartTimestamp = (toStartTimestamp * 0.9).toLong()
            toEndTimestamp = (toEndTimestamp * 1.1).toLong()

            fromValues = toValues
            fromStartTimestamp = toStartTimestamp
            fromEndTimestamp = toEndTimestamp
            fromMinValue = toMinValue
            fromMaxValue = toMaxValue
        }

        frameValues = fromValues
        frameStartTimestamp = fromStartTimestamp
        frameEndTimestamp = fromEndTimestamp
        frameMinValue = fromMinValue
        frameMaxValue = fromMaxValue

        fromValuesFilled = fillWith(fromValues, toValues)
        toValuesFilled = fillWith(toValues, fromValues)

//        matchTimestamps = matchTimestamps(fromValues.keys.toList(), toValues.keys.toList())
    }

    fun nextFrame(animatedFraction: Float) {
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

//            matchTimestamps.forEach { (t1, t2) ->
//                val valueFrom = fromValues[t1]!!
//                val valueTo = toValues[t2]!!
//
//                val t3 =  getForFrame(t1.toFloat(), t2.toFloat(), animatedFraction).toLong()
//
//                values[t3] = getForFrame(valueFrom, valueTo, animatedFraction)
//
//            }

            frameValues = LinkedHashMap(values.toSortedMap())
        }
    }

    private fun getForFrame(start: Float, end: Float, animatedFraction: Float): Float {
        val change = end - start

        return start + (change * animatedFraction)
    }

    fun getFramePoints(extraVerticalOffset: Float = 0f): List<ChartPointF> {
        // timestamp = ax + startTimestamp
        // x = (timestamp - startTimestamp) / a
        // a = (timestamp - startTimestamp) / x
        val xRatio = (frameEndTimestamp - frameStartTimestamp) / (xMax - horizontalOffset * 2)

        // value = ay + minValue
        // y = (value - minValue) / a
        // a = (value - minValue) / y
        val yRatio = (frameMaxValue - frameMinValue) / (yMax - curveTopOffset - curveBottomOffset - 2 * extraVerticalOffset)

        return frameValues.map { (timestamp, value) ->
            val x = (timestamp - frameStartTimestamp) / xRatio + horizontalOffset
            val y = (value - frameMinValue) / yRatio + curveBottomOffset + extraVerticalOffset

            val y2 = (y * -1) + yMax

            ChartPointF(x, y2)
        }
    }


    companion object {
        fun matchTimestamps(
            timestampsFrom: List<Long>,
            timestampsTo: List<Long>
        ): List<Pair<Long, Long>> {

            val result = mutableListOf<Pair<Long, Long>>()

            val timestampsFromMutable = timestampsFrom.toMutableList()
            val timestampsToMutable = timestampsTo.toMutableList()

            var t1 = timestampsFromMutable.removeFirst()
            var t2 = timestampsToMutable.removeFirst()

            while (true) {
                result.add(t1 to t2)

                if (timestampsFromMutable.isEmpty() && timestampsToMutable.isEmpty()) {
                    break
                }

                if (t1 == t2) {
                    if (timestampsFromMutable.isNotEmpty()) {
                        t1 = timestampsFromMutable.removeFirst()
                    }
                    if (timestampsToMutable.isNotEmpty()) {
                        t2 = timestampsToMutable.removeFirst()
                    }
                }
                else if (t1 < t2) {
                    if (timestampsFromMutable.isNotEmpty()) {
                        t1 = timestampsFromMutable.removeFirst()
                    } else if (timestampsToMutable.isNotEmpty()) {
                        t2 = timestampsToMutable.removeFirst()
                    }
                } else if (t1 > t2) {
                    if (timestampsToMutable.isNotEmpty()) {
                        t2 = timestampsToMutable.removeFirst()
                    } else if (timestampsFromMutable.isNotEmpty()) {
                        t1 = timestampsFromMutable.removeFirst()
                    }
                }
            }

            return result
        }

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
