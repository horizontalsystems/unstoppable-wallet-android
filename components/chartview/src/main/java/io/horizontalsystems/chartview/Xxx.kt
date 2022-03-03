package io.horizontalsystems.chartview

import android.util.Log
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartPointF

class Yyy(
    private val animator: ChartAnimator?,

    private val fromPointsMap: LinkedHashMap<Long, ChartPointF>,
    private val fromStartTimestamp: Long,
    private val fromEndTimestamp: Long,
    private val toPointsMap: LinkedHashMap<Long, ChartPointF>,
    private val toStartTimestamp: Long,
    private val toEndTimestamp: Long,
) {
    private val fromPointsMapFilled: LinkedHashMap<Long, ChartPointF>
    private val toPointsMapFilled: LinkedHashMap<Long, ChartPointF>

    private var currentFramePointsMap = linkedMapOf<Long, ChartPointF>()
    private var currentFrameStartTimestamp = 0L
    private var currentFrameEndTimestamp = 0L

    init {
        Log.e("AAA", "init XxxYyy")
        if (fromPointsMap.isNotEmpty() && toPointsMap.isNotEmpty()) {
            fromPointsMapFilled = fillWith(fromPointsMap, toPointsMap, fromStartTimestamp)
            toPointsMapFilled = fillWith(toPointsMap, fromPointsMap, toStartTimestamp)
        } else {
            fromPointsMapFilled = fromPointsMap
            toPointsMapFilled = toPointsMap
        }
    }

    fun nextFrame() {
        if (fromPointsMapFilled.isEmpty() || animator == null) {
            currentFramePointsMap = toPointsMap
            currentFrameStartTimestamp = toStartTimestamp
            currentFrameEndTimestamp = toEndTimestamp
        } else {
            val animatedFraction = animator.animatedFraction

            val currentStartTimeStamp = getForFrame(
                fromStartTimestamp.toFloat(),
                toStartTimestamp.toFloat(),
                animatedFraction
            ).toLong()
            val currentEndTimeStamp = getForFrame(
                fromEndTimestamp.toFloat(),
                toEndTimestamp.toFloat(),
                animatedFraction
            ).toLong()

            val currentPointsMap = mutableMapOf<Long, ChartPointF>()
            for ((timestamp, prevPoint) in fromPointsMapFilled) {
                if (timestamp < currentStartTimeStamp || timestamp > currentEndTimeStamp) continue

                val nextPoint = toPointsMapFilled[timestamp]!!

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

companion object {
    fun fillWith(
        prevPointsMap: LinkedHashMap<Long, ChartPointF>,
        nextPointsMap: LinkedHashMap<Long, ChartPointF>,
        prevStartTimestamp: Long,
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
            if (!prevPointsMutableMap.containsKey(timestamp)) {
                val x = (timestamp - prevStartTimestamp) / prevA

                val timeStampBefore = prevTimestamps.lastOrNull { it < timestamp } ?: continue
                val timeStampAfter = prevTimestamps.firstOrNull { it > timestamp } ?: continue

                // y = (x - x1) * (y2 - y1) / (x2 - x1) + y1

                val pointBefore = prevPointsMutableMap[timeStampBefore]!!
                val pointAfter = prevPointsMutableMap[timeStampAfter]!!

                val x1 = pointBefore.x
                val y1 = pointBefore.y
                val x2 = pointAfter.x
                val y2 = pointAfter.y

                val y = (x - x1) * (y2 - y1) / (x2 - x1) + y1

                prevPointsMutableMap[timestamp] = ChartPointF(x, y)
            }
        }

//        val filledXs = prevPointsMutableMap.toSortedMap().map { it.value.x }
//        if (filledXs != filledXs.sorted()) {
//
//            val fromOriginalXs = prevPointsMap.map { it.value.x }
//
//            val originalXsOrdered = fromOriginalXs == fromOriginalXs.sorted()
//
//            Log.e("AAA", "Not ordered: ${filledXs}")
//            Log.e("AAA", "Ordering of original: $originalXsOrdered")
//
//
//            var prevPointsMapExport = "\nval prevPointsMap\n"
//            prevPointsMapExport += prevPointsMap.map { (t, p) ->
//                "$t to ChartPointF(${p.x}f, ${p.y}f),"
//            }.joinToString("\n")
//
//            Log.e("AAA", "\n\n$prevPointsMapExport\n\n")
//
//            var nextPointsMapExport = "\nval nextPointsMap\n"
//            nextPointsMapExport += nextPointsMap.map { (t, p) ->
//                "$t to ChartPointF(${p.x}f, ${p.y}f),"
//            }.joinToString("\n")
//
//            Log.e("AAA", "\n\n$nextPointsMapExport\n\n")
//            Log.e("AAA", "\n\nprevStartTimestamp = $prevStartTimestamp\n\n")
//
//            throw Exception("Stope")
//        }


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

}