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
    private val appear: ChartTransition,
) {
    var color: Long? = null

    private var target = CurveFrame(toValues, toStartTimestamp, toEndTimestamp, toMinValue, toMaxValue)

    // A new curve has no old frame to leave, so it plays the appear transition from its own
    // data, and already shows that transition's first frame before the animation starts.
    private var frameAt: (Float) -> CurveFrame = appear.line(target.copy(alpha = 0f), target)
    private var frame = frameAt(0f)
    private var appeared = false

    data class UiState(
        val values: LinkedHashMap<Long, Float>,
        val startTimestamp: Long,
        val endTimestamp: Long,
        val minValue: Float,
        val maxValue: Float,
        val alpha: Float,
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
        frameAt = when {
            !appeared -> appear.line(to.copy(alpha = 0f), to)
            frame.values.isEmpty() -> { _ -> to }
            else -> transition.line(frame, to)
        }
    }

    /**
     * Plays [disappear] on what is on screen. The curve does not remove itself: its owner
     * drops it when the animation ends.
     */
    fun leave(disappear: ChartTransition) {
        frameAt = disappear.line(frame, frame)
        target = frameAt(1f)
    }

    fun onNextFrame(animatedFraction: Float) {
        // The last frame is the target itself, not a computed copy that may differ by rounding.
        frame = if (animatedFraction == 1f) target else frameAt(animatedFraction)
        appeared = true

        state = uiState()
    }

    private fun uiState() = UiState(
        values = frame.values,
        startTimestamp = frame.startTimestamp,
        endTimestamp = frame.endTimestamp,
        minValue = frame.minValue,
        maxValue = frame.maxValue,
        alpha = frame.alpha,
        color = color,
    )
}
