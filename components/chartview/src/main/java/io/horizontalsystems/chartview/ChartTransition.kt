package io.horizontalsystems.chartview

import kotlin.math.roundToLong

/**
 * What a chart layer shows at one moment: its points, the time and value ranges the
 * drawing area spans, and how opaque it is drawn.
 */
data class CurveFrame(
    val values: LinkedHashMap<Long, Float>,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val minValue: Float,
    val maxValue: Float,
    val alpha: Float = 1f,
)

/**
 * How a chart layer gets from the frame on screen to a new one.
 *
 * A transition only decides the path between the two frames. Timing (duration, easing,
 * bounce) belongs to the animation spec that produces the fraction. The returned function
 * takes that fraction, 0 at the start and 1 at the end, and may be called with values
 * outside 0..1 by a spec that overshoots.
 *
 * A layer with nothing on screen yet is given its new frame at alpha 0 as the old one.
 */
interface ChartTransition {
    fun line(from: CurveFrame, to: CurveFrame): (Float) -> CurveFrame

    fun bars(from: CurveFrame, to: CurveFrame): (Float) -> CurveFrame = line(from, to)

    /**
     * The new ranges apply at once and every point of the new data moves straight up or down
     * into place, starting from where the old line crossed the same horizontal position.
     *
     * A line also gets a point wherever the old line had one, so a peak of the old line that
     * falls between two new points is there on the first frame and moves away with the rest.
     * Those points lie on the new line at the end. Bars get none: each would be a bar more.
     */
    object Morph : ChartTransition {
        override fun line(from: CurveFrame, to: CurveFrame) = morph(from, to, keepOldPoints = true)

        override fun bars(from: CurveFrame, to: CurveFrame) = morph(from, to, keepOldPoints = false)

        private fun morph(from: CurveFrame, to: CurveFrame, keepOldPoints: Boolean): (Float) -> CurveFrame {
            val fromTimeRange = (from.endTimestamp - from.startTimestamp).toDouble()
            val toTimeRange = (to.endTimestamp - to.startTimestamp).toDouble()
            val fromValueRange = from.maxValue - from.minValue
            val toValueRange = to.maxValue - to.minValue

            if (from.values.isEmpty() || toTimeRange == 0.0 || fromValueRange == 0f) {
                return { to }
            }

            val endValues = if (keepOldPoints && fromTimeRange > 0.0 && to.values.size > 1) {
                withPointsOf(from, to, fromTimeRange, toTimeRange)
            } else {
                to.values
            }

            val sampler = Sampler(from.values)
            val startValues = endValues.mapValues { (timestamp, _) ->
                val position = (timestamp - to.startTimestamp) / toTimeRange
                val fromValue = sampler.at(from.startTimestamp + position * fromTimeRange)

                to.minValue + (fromValue - from.minValue) / fromValueRange * toValueRange
            }

            return { fraction ->
                if (fraction == 1f) {
                    to
                } else {
                    to.copy(
                        values = endValues.mapValuesTo(LinkedHashMap()) { (timestamp, value) ->
                            lerp(startValues.getValue(timestamp), value, fraction)
                        },
                        // A layer caught half faded gets the rest of the way with its new data.
                        alpha = lerp(from.alpha, to.alpha, fraction).coerceIn(0f, 1f)
                    )
                }
            }
        }

        /** The new line with a point added at every horizontal position the old line had one. */
        private fun withPointsOf(
            from: CurveFrame,
            to: CurveFrame,
            fromTimeRange: Double,
            toTimeRange: Double,
        ): Map<Long, Float> {
            val toSampler = Sampler(to.values)
            val first = to.values.keys.min()
            val last = to.values.keys.max()
            val merged = sortedMapOf<Long, Float>()
            merged.putAll(to.values)

            from.values.keys.forEach { fromTimestamp ->
                val position = (fromTimestamp - from.startTimestamp) / fromTimeRange
                val timestamp = (to.startTimestamp + position * toTimeRange).roundToLong()
                // Only inside the new line: it is not drawn beyond its first and last points.
                if (timestamp in (first + 1) until last && timestamp !in merged) {
                    merged[timestamp] = toSampler.at(timestamp.toDouble())
                }
            }

            return merged
        }
    }

    /**
     * The new line starts flat on the baseline and grows to its shape. The old line is
     * dropped at once.
     */
    object Rise : ChartTransition {
        override fun line(from: CurveFrame, to: CurveFrame): (Float) -> CurveFrame = { fraction ->
            grown(to, fraction)
        }
    }

    /**
     * The new line is in place from the start and goes from the opacity on screen to its own.
     * It is there once the fraction reaches [completeAt], so a value below 1 makes the fade
     * shorter than the animation it is part of.
     */
    class FadeIn(private val completeAt: Float = 1f) : ChartTransition {
        override fun line(from: CurveFrame, to: CurveFrame): (Float) -> CurveFrame = { fraction ->
            to.copy(alpha = lerp(from.alpha, to.alpha, (fraction / completeAt).coerceIn(0f, 1f)))
        }
    }

    /**
     * The old line stays in place and goes transparent, from however opaque it was. Nothing of
     * the new data is shown: this is how a layer leaves the chart. It is gone once the fraction
     * reaches [completeAt].
     */
    class FadeOut(private val completeAt: Float = 1f) : ChartTransition {
        override fun line(from: CurveFrame, to: CurveFrame): (Float) -> CurveFrame = { fraction ->
            from.copy(alpha = from.alpha * (1f - (fraction / completeAt).coerceIn(0f, 1f)))
        }
    }
}

/** [frame] with every value brought towards its baseline: on it at 0, in place at 1. */
private fun grown(frame: CurveFrame, share: Float): CurveFrame {
    // Zero where the range includes it, so bars below zero grow downwards; else the bottom.
    val baseline = if (frame.minValue <= frame.maxValue) {
        0f.coerceIn(frame.minValue, frame.maxValue)
    } else {
        frame.minValue
    }

    return frame.copy(
        values = frame.values.mapValuesTo(LinkedHashMap()) { (_, value) ->
            lerp(baseline, value, share)
        }
    )
}

/** Reads a line at timestamps that fall between its points. */
private class Sampler(line: Map<Long, Float>) {
    private val timestamps = line.keys.sorted().toLongArray()
    private val values = FloatArray(timestamps.size) { line.getValue(timestamps[it]) }

    /** The line's value at [timestamp], holding the first and last values beyond its ends. */
    fun at(timestamp: Double): Float = when {
        timestamp <= timestamps.first() -> values.first()
        timestamp >= timestamps.last() -> values.last()
        else -> between(timestamp)
    }

    private fun between(timestamp: Double): Float {
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
