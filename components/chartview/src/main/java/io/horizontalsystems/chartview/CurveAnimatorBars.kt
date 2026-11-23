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
    private val appear: ChartTransition,
) {
    private var target = CurveFrame(targetValues, targetMinKey, targetMaxKey, targetMinValue, targetMaxValue)

    // New bars have no old frame to leave, so they play the appear transition from their own
    // data, and already show that transition's first frame before the animation starts.
    private var frameAt: (Float) -> CurveFrame = appear.bars(target, target)
    private var frame = frameAt(0f)
    private var appeared = false

    data class UiState(
        val values: LinkedHashMap<Long, Float>,
        val startTimestamp: Long,
        val endTimestamp: Long,
        val minValue: Float,
        val maxValue: Float,
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
            !appeared -> appear.bars(to, to)
            animated -> transition.bars(frame, to)
            else -> { _ -> to }
        }
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
    )
}
