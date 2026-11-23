package io.horizontalsystems.chartview

import org.junit.Assert.assertEquals
import org.junit.Test

class CurveAnimatorBarsTest {

    private val values = linkedMapOf(0L to 1f, 100L to 3f)

    private fun shownBars() = CurveAnimatorBars(
        values, 0, 100, 0f, 3f,
        transition = ChartTransition.Morph,
        appear = ChartTransition.Rise,
    ).apply {
        setValues(values, 0, 100, 0f, 3f)
        onNextFrame(1f)
    }

    @Test
    fun reappear_comesBackFromTheOpacityAHideWasCutOffAt() {
        val bars = shownBars()
        bars.leave(ChartTransition.FadeOut())
        bars.onNextFrame(0.5f)

        bars.reappear(ChartTransition.FadeIn(), stillShown = true)
        bars.setValues(values, 0, 100, 0f, 3f)

        assertEquals(0.5f, bars.state.alpha)
        bars.onNextFrame(0.5f)
        assertEquals(0.75f, bars.state.alpha)
        bars.onNextFrame(1f)
        assertEquals(1f, bars.state.alpha)
    }

    @Test
    fun reappear_comesBackFromNothingAfterAFinishedHide() {
        val bars = shownBars()
        bars.leave(ChartTransition.FadeOut())
        bars.onNextFrame(1f)
        // Data that came while the bars were out of sight.
        bars.setValues(values, 0, 100, 0f, 3f)
        bars.onNextFrame(1f)

        bars.reappear(ChartTransition.FadeIn(), stillShown = false)
        bars.setValues(values, 0, 100, 0f, 3f)

        assertEquals(0f, bars.state.alpha)
        bars.onNextFrame(0.5f)
        assertEquals(0.5f, bars.state.alpha)
    }
}
