package io.horizontalsystems.chartview

import android.util.Log
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartPointF

class Yyy(private val animator: ChartAnimator?) {

    private var fromPointsMap = linkedMapOf<Long, ChartPointF>()
    private var fromStartTimestamp = 0L
    private var fromEndTimestamp = 0L

    private var toPointsMap = linkedMapOf<Long, ChartPointF>()
    private var toStartTimestamp = 0L
    private var toEndTimestamp = 0L

    private var currentFramePointsMap = linkedMapOf<Long, ChartPointF>()
    private var currentFrameStartTimestamp = 0L
    private var currentFrameEndTimestamp = 0L

    fun setTransitionFrom(
        pointsMap: LinkedHashMap<Long, ChartPointF>,
        startTimestamp: Long,
        endTimestamp: Long
    ) {
        fromPointsMap = pointsMap
        fromStartTimestamp = startTimestamp
        fromEndTimestamp = endTimestamp
    }

    fun setTransitionTo(
        pointsMap: LinkedHashMap<Long, ChartPointF>,
        startTimestamp: Long,
        endTimestamp: Long
    ) {
        toPointsMap = pointsMap
        toStartTimestamp = startTimestamp
        toEndTimestamp = endTimestamp
    }

    fun calculate() {
        if (fromPointsMap.isNotEmpty() && toPointsMap.isNotEmpty()) {
            val fromFilled = fillWith(fromPointsMap, toPointsMap, fromStartTimestamp)
            val toFilled = fillWith(toPointsMap, fromPointsMap, toStartTimestamp)

            fromPointsMap = fromFilled
            toPointsMap = toFilled
        }
    }

    fun nextFrame() {
        if (fromPointsMap.isEmpty() || animator == null) {
            currentFramePointsMap = toPointsMap
            currentFrameStartTimestamp = toStartTimestamp
            currentFrameEndTimestamp = toEndTimestamp
        } else {
            val animatedFraction = animator.animatedFraction

            Log.e("AAA", "animatedFraction: $animatedFraction")

            val currentStartTimeStamp = getForFrame(fromStartTimestamp.toFloat(), toStartTimestamp.toFloat(), animatedFraction).toLong()
            val currentEndTimeStamp = getForFrame(fromEndTimestamp.toFloat(), toEndTimestamp.toFloat(), animatedFraction).toLong()

            val currentPointsMap = mutableMapOf<Long, ChartPointF>()
            for ((timestamp, prevPoint) in fromPointsMap) {
                if (timestamp < currentStartTimeStamp || timestamp > currentEndTimeStamp) continue

                val nextPoint = toPointsMap[timestamp]!!

                val currentX = getForFrame(prevPoint.x, nextPoint.x, animatedFraction)
                val currentY = getForFrame(prevPoint.y, nextPoint.y, animatedFraction)

                currentPointsMap[timestamp] = ChartPointF(currentX, currentY)
            }

            currentFramePointsMap = LinkedHashMap(currentPointsMap.toSortedMap())
            currentFrameStartTimestamp = currentStartTimeStamp
            currentFrameEndTimestamp = currentEndTimeStamp
        }
    }

    fun getCurrentFramePoints(): LinkedHashMap<Long, ChartPointF> {
        return currentFramePointsMap
    }

    fun getCurrentFrameStartTimestamp(): Long {
        return currentFrameStartTimestamp
    }

    fun getCurrentFrameEndTimestamp(): Long {
        return currentFrameEndTimestamp
    }

    private fun getForFrame(start: Float, end: Float, animatedFraction: Float): Float {
        val change = end - start

        return start + (change * animatedFraction)
    }

    fun fillWith(
        prevPointsMap: LinkedHashMap<Long, ChartPointF>,
        nextPointsMap: LinkedHashMap<Long, ChartPointF>,
        prevStartTimestamp: Long
    ): LinkedHashMap<Long, ChartPointF> {
        val prevPointsMutableMap = prevPointsMap.toMutableMap()

        val prevTimestamps = prevPointsMap.keys

        // timestamp = ax + b
        // when x = 0, timestamp is minimum, i.e. startTimestamp
        // b = startTimestamp
        // timestamp = ax + startTimestamp
        // x = (timestamp - startTimestamp) / a
        val prevA = getA(prevPointsMap, prevStartTimestamp)

        for ((timestamp, point) in nextPointsMap) {
            if (!prevPointsMap.containsKey(timestamp)) {
                val x = (timestamp - prevStartTimestamp) / prevA

                val timeStampBefore = prevTimestamps.filter { it < timestamp }.lastOrNull() ?: continue
                val timeStampAfter = prevTimestamps.filter { it > timestamp }.firstOrNull() ?: continue

                // y = (x - x1) * (y2 - y1) / (x2 - x1) + y1

                val pointBefore = prevPointsMap[timeStampBefore]!!
                val pointAfter = prevPointsMap[timeStampAfter]!!

                val x1 = pointBefore.x
                val x2 = pointAfter.x
                val y1 = pointBefore.y
                val y2 = pointAfter.y

                val y = (x - x1) * (y2 - y1) / (x2 - x1) + y1

                prevPointsMutableMap[timestamp] = ChartPointF(x, y)
            }
        }

        val firstIntersectTimeStamp = nextPointsMap.keys.first { prevPointsMutableMap.containsKey(it) }
        // prevY = nextY / yRatio
        val yRatio = nextPointsMap[firstIntersectTimeStamp]!!.y / prevPointsMutableMap[firstIntersectTimeStamp]!!.y

        for ((timestamp, point) in nextPointsMap) {
            if (!prevPointsMutableMap.containsKey(timestamp)) {
                val x = (timestamp - prevStartTimestamp) / prevA
                val y = point.y / yRatio

                prevPointsMutableMap[timestamp] = ChartPointF(x, y)
            }
        }

        return LinkedHashMap(prevPointsMutableMap.toSortedMap())
    }

    // a = (timestamp - startTimestamp) / x
    private fun getA(
        pointsMap: LinkedHashMap<Long, ChartPointF>,
        startTimestamp: Long
    ): Float {
        // calculate a from any point
        val first = pointsMap.entries.first { it.value.x != 0f }
        val timestamp = first.key
        val x = first.value.x

        return (timestamp - startTimestamp) / x
    }

}