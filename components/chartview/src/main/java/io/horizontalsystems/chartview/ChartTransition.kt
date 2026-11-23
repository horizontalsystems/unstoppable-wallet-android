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

    /**
     * The new line starts flat on the baseline and grows to its shape. The old line is
     * dropped at once.
     */
    object Rise : ChartTransition {
        override fun line(from: CurveFrame, to: CurveFrame): (Float) -> CurveFrame {
            // Zero where the range includes it, so bars below zero grow downwards; else the bottom.
            val baseline = if (to.minValue <= to.maxValue) {
                0f.coerceIn(to.minValue, to.maxValue)
            } else {
                to.minValue
            }

            return { fraction ->
                to.copy(
                    values = to.values.mapValuesTo(LinkedHashMap()) { (_, value) ->
                        lerp(baseline, value, fraction)
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
