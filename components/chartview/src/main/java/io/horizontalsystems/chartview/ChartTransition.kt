package io.horizontalsystems.chartview

/**
 * What a chart layer shows at one moment: its points and the time and value ranges the
 * drawing area spans.
 */
data class CurveFrame(
    val values: LinkedHashMap<Long, Float>,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val minValue: Float,
    val maxValue: Float,
)

/**
 * How a chart layer gets from the frame on screen to a new one.
 *
 * A transition only decides the path between the two frames. Timing (duration, easing,
 * bounce) belongs to the animation spec that produces the fraction. The returned function
 * takes that fraction, 0 at the start and 1 at the end, and may be called with values
 * outside 0..1 by a spec that overshoots.
 */
interface ChartTransition {
    fun line(from: CurveFrame, to: CurveFrame): (Float) -> CurveFrame

    fun bars(from: CurveFrame, to: CurveFrame): (Float) -> CurveFrame = line(from, to)

    /** The new data replaces the old at once. */
    object None : ChartTransition {
        override fun line(from: CurveFrame, to: CurveFrame): (Float) -> CurveFrame = { to }
    }

    /**
     * The time and value ranges slide from the old ones to the new ones, so the old line is
     * moved and rescaled like a camera pan and zoom, while points both lines cover move to
     * their new values.
     */
    object MoveAndScale : ChartTransition {
        override fun line(from: CurveFrame, to: CurveFrame): (Float) -> CurveFrame {
            val fromValues = fillWith(from.values, to.values)
            val toValues = fillWith(to.values, from.values)

            return { fraction ->
                frame(from, to, fraction) {
                    fromValues.mapValuesTo(LinkedHashMap()) { (timestamp, value) ->
                        lerp(value, toValues.getValue(timestamp), fraction)
                    }
                }
            }
        }

        // A bar has no neighbours to interpolate between, so one that exists on only one side
        // grows from, or shrinks to, a stub below the range.
        override fun bars(from: CurveFrame, to: CurveFrame): (Float) -> CurveFrame {
            val timestamps = (from.values.keys + to.values.keys).sorted()
            val fromStub = from.minValue / 10
            val toStub = to.minValue / 10

            return { fraction ->
                frame(from, to, fraction) {
                    timestamps.associateWithTo(LinkedHashMap()) { timestamp ->
                        lerp(
                            from.values[timestamp] ?: fromStub,
                            to.values[timestamp] ?: toStub,
                            fraction
                        )
                    }
                }
            }
        }

        private fun frame(
            from: CurveFrame,
            to: CurveFrame,
            fraction: Float,
            values: () -> LinkedHashMap<Long, Float>,
        ) = CurveFrame(
            values = values(),
            startTimestamp = lerp(from.startTimestamp, to.startTimestamp, fraction),
            endTimestamp = lerp(from.endTimestamp, to.endTimestamp, fraction),
            minValue = lerp(from.minValue, to.minValue, fraction),
            maxValue = lerp(from.maxValue, to.maxValue, fraction),
        )

        /**
         * [values] with a point added at every timestamp of [other]: interpolated where the
         * timestamp falls between two of its own points, taken from [other] where it lies
         * outside them.
         */
        internal fun fillWith(
            values: LinkedHashMap<Long, Float>,
            other: LinkedHashMap<Long, Float>,
        ): LinkedHashMap<Long, Float> {
            val sampler = Sampler(values)
            val filled = values.toMutableMap()

            for ((timestamp, otherValue) in other) {
                if (!filled.containsKey(timestamp)) {
                    filled[timestamp] = sampler.between(timestamp) ?: otherValue
                }
            }

            return LinkedHashMap(filled.toSortedMap())
        }
    }

    /**
     * The new ranges apply at once and every point of the new data moves straight up or down
     * into place, starting from where the old line crossed the same horizontal position.
     */
    object Morph : ChartTransition {
        override fun line(from: CurveFrame, to: CurveFrame): (Float) -> CurveFrame {
            val fromTimeRange = (from.endTimestamp - from.startTimestamp).toDouble()
            val toTimeRange = (to.endTimestamp - to.startTimestamp).toDouble()
            val fromValueRange = from.maxValue - from.minValue
            val toValueRange = to.maxValue - to.minValue

            if (from.values.isEmpty() || toTimeRange == 0.0 || fromValueRange == 0f) {
                return { to }
            }

            val sampler = Sampler(from.values)
            val startValues = to.values.mapValues { (timestamp, _) ->
                val position = (timestamp - to.startTimestamp) / toTimeRange
                val fromValue = sampler.at(from.startTimestamp + position * fromTimeRange)

                to.minValue + (fromValue - from.minValue) / fromValueRange * toValueRange
            }

            return { fraction ->
                to.copy(
                    values = to.values.mapValuesTo(LinkedHashMap()) { (timestamp, value) ->
                        lerp(startValues.getValue(timestamp), value, fraction)
                    }
                )
            }
        }
    }
}

/** Reads a line at timestamps that fall between its points. */
private class Sampler(line: Map<Long, Float>) {
    private val timestamps = line.keys.sorted().toLongArray()
    private val values = FloatArray(timestamps.size) { line.getValue(timestamps[it]) }

    /** The line's value at [timestamp], or null when it is not strictly inside the line. */
    fun between(timestamp: Long): Float? {
        if (timestamps.isEmpty() || timestamp <= timestamps.first() || timestamp >= timestamps.last()) {
            return null
        }
        return at(timestamp.toDouble())
    }

    /** The line's value at [timestamp], holding the first and last values beyond its ends. */
    fun at(timestamp: Double): Float {
        if (timestamp <= timestamps.first()) return values.first()
        if (timestamp >= timestamps.last()) return values.last()

        var low = 0
        var high = timestamps.lastIndex
        while (high - low > 1) {
            val middle = (low + high) / 2
            if (timestamps[middle] <= timestamp) low = middle else high = middle
        }

        val share = (timestamp - timestamps[low]) / (timestamps[high] - timestamps[low])
        return (values[low] + (values[high] - values[low]) * share).toFloat()
    }
}

private fun lerp(start: Float, end: Float, fraction: Float) = start + (end - start) * fraction

// In Double, because a Float cannot hold a Unix timestamp to better than about two minutes.
private fun lerp(start: Long, end: Long, fraction: Float) =
    (start + (end - start) * fraction.toDouble()).toLong()
