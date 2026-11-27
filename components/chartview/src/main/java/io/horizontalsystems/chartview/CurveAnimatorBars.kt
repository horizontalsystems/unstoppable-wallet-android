package io.horizontalsystems.chartview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class CurveAnimatorBars(
    targetValues: LinkedHashMap<Long, Float>,
    targetMinKey: Long,
    targetMaxKey: Long,
    targetMinValue: Float,
    targetMaxValue: Float,
    private val transition: ChartTransition,
    private var appear: ChartTransition,
) {
    private var target = CurveFrame(targetValues, targetMinKey, targetMaxKey, targetMinValue, targetMaxValue)

    // New bars have no old frame to leave, so they play the appear transition from their own
    // data, and already show that transition's first frame before the animation starts.
    private var frameAt: (Float) -> CurveFrame = appear.bars(target.copy(alpha = 0f), target)
    private var frame = frameAt(0f)
    private var appeared = false

    // How opaque the bars are on screen while they have not appeared: not at all, unless
    // they come back before they were gone.
    private var shownAlpha = 0f

    data class UiState(
        val values: LinkedHashMap<Long, Float>,
        val startTimestamp: Long,
        val endTimestamp: Long,
        val minValue: Float,
        val maxValue: Float,
        val alpha: Float,
    )

    var state by mutableStateOf(uiState())
        private set

    fun setValues(
        values: LinkedHashMap<Long, Float>,
        minKey: Long,
        maxKey: Long,
        minValue: Float = values.minOfOrNull { it.value } ?: 0f,
        maxValue: Float = values.maxOfOrNull { it.value } ?: 0f,
    ) {
        // A single timestamp has no width to animate across, so it is shown at once.
        val animated = minKey != maxKey && frame.values.isNotEmpty()

        val to = CurveFrame(
            values = values,
            startTimestamp = if (minKey == maxKey) (minKey * 0.9).toLong() else minKey,
            endTimestamp = if (minKey == maxKey) (maxKey * 1.1).toLong() else maxKey,
            minValue = if (minValue == maxValue) minValue * 0.9f else minValue,
            maxValue = maxValue,
        )

        target = to
        frameAt = when {
            !appeared -> appear.bars(to.copy(alpha = shownAlpha), to)
            animated -> transition.bars(frame, to)
            else -> { _ -> to }
        }

        if (!appeared) {
            // Bars coming back still hold the frame they left with. Show the first frame of
            // their return now, so the old one is not drawn before the animation starts.
            frame = frameAt(0f)
            state = uiState()
        }
    }

    /**
     * Plays [disappear] on what is on screen. The bars do not remove themselves: their owner
     * drops them when the animation ends.
     */
    fun leave(disappear: ChartTransition) {
        frameAt = disappear.bars(frame, frame)
        target = frameAt(1f)
    }

    /**
     * Makes the next data come in with [appear], for bars that come back into view.
     * [stillShown] says they were on their way out and are partly visible, so they come back
     * from the opacity they have; otherwise from nothing.
     */
    fun reappear(appear: ChartTransition, stillShown: Boolean) {
        this.appear = appear
        shownAlpha = if (stillShown) frame.alpha else 0f
        appeared = false
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
    )
}
