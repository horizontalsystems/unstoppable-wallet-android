package io.horizontalsystems.chartview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class CurveAnimator2(
    toValues: LinkedHashMap<Long, Float>,
    toStartTimestamp: Long,
    toEndTimestamp: Long,
    toMinValue: Float,
    toMaxValue: Float,
    private val transition: ChartTransition,
) {
    var color: Long? = null

    private var target = CurveFrame(toValues, toStartTimestamp, toEndTimestamp, toMinValue, toMaxValue)
    private var frame = target
    private var frameAt: (Float) -> CurveFrame = { target }

    data class UiState(
        val values: LinkedHashMap<Long, Float>,
        val startTimestamp: Long,
        val endTimestamp: Long,
        val minValue: Float,
        val maxValue: Float,
        val color: Long?
    )

    var state by mutableStateOf(uiState())
        private set

    fun setTo(
        toValues: LinkedHashMap<Long, Float>,
        toStartTimestamp: Long,
        toEndTimestamp: Long,
        toMinValue: Float,
        toMaxValue: Float,
    ) {
        val to = CurveFrame(toValues, toStartTimestamp, toEndTimestamp, toMinValue, toMaxValue)

        target = to
        frameAt = if (frame.values.isEmpty()) {
            { to }
        } else {
            transition.line(frame, to)
        }
    }

    fun onNextFrame(animatedFraction: Float) {
        // The last frame is the target itself, not a computed copy that may differ by rounding.
        frame = if (animatedFraction == 1f) target else frameAt(animatedFraction)

        state = uiState()
    }

    private fun uiState() = UiState(
        values = frame.values,
        startTimestamp = frame.startTimestamp,
        endTimestamp = frame.endTimestamp,
        minValue = frame.minValue,
        maxValue = frame.maxValue,
        color = color,
    )
}
