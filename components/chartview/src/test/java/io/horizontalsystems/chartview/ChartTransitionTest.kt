package io.horizontalsystems.chartview

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartTransitionTest {

    private fun frame(start: Long, end: Long, min: Float, max: Float, vararg points: Pair<Long, Float>) =
        CurveFrame(linkedMapOf(*points), start, end, min, max)

    private val day = frame(100, 200, 0f, 10f, 100L to 0f, 150L to 10f, 200L to 5f)


    @Test
    fun morph_usesTheNewRangesAndTimestampsFromTheFirstFrame() {
        val to = frame(0, 200, 0f, 20f, 0L to 20f, 100L to 0f, 200L to 5f)

        val first = ChartTransition.Morph.line(day, to)(0f)

        assertEquals(0, first.startTimestamp)
        assertEquals(200, first.endTimestamp)
        assertEquals(0f, first.minValue)
        assertEquals(20f, first.maxValue)
        assertEquals(to.values.keys.toList(), first.values.keys.toList())
    }

    @Test
    fun morph_startsWhereTheOldLineWasOnScreen() {
        val to = frame(0, 200, 0f, 20f, 0L to 20f, 50L to 20f, 100L to 0f, 200L to 5f)

        val first = ChartTransition.Morph.line(day, to)(0f)

        // The left edge showed the old value 0, the bottom of the area: the bottom is 0 now.
        assertEquals(0f, first.values.getValue(0))
        // A quarter of the way across, the old line was halfway up: 10 of the new 0..20.
        assertEquals(10f, first.values.getValue(50))
        // The middle showed the old maximum, the top of the area: the top is 20 now.
        assertEquals(20f, first.values.getValue(100))
        // The right edge showed 5 of 0..10, halfway up.
        assertEquals(10f, first.values.getValue(200))
    }

    @Test
    fun morph_movesEachPointStraightToItsNewValue() {
        val to = frame(0, 200, 0f, 20f, 0L to 20f, 100L to 0f, 200L to 5f)
        val frameAt = ChartTransition.Morph.line(day, to)

        assertEquals(linkedMapOf(0L to 10f, 100L to 10f, 200L to 7.5f), frameAt(0.5f).values)
        assertEquals(to, frameAt(1f))
    }

    @Test
    fun morph_holdsTheEndValuesOfAnOldLineShorterThanItsRange() {
        val from = frame(0, 100, 0f, 10f, 50L to 4f, 100L to 8f)
        val to = frame(0, 100, 0f, 10f, 0L to 1f, 100L to 1f)

        val first = ChartTransition.Morph.line(from, to)(0f)

        assertEquals(4f, first.values.getValue(0))
        assertEquals(8f, first.values.getValue(100))
    }

    @Test
    fun morph_showsTheNewDataAtOnceWhenTheOldFrameHasNoHeight() {
        val from = frame(0, 100, 5f, 5f, 0L to 5f, 100L to 5f)
        val to = frame(0, 100, 0f, 10f, 0L to 1f, 100L to 9f)

        assertEquals(to, ChartTransition.Morph.line(from, to)(0f))
    }

    @Test
    fun rise_startsFlatOnTheBottomOfARangeAboveZero() {
        val to = frame(0, 100, 2f, 10f, 0L to 4f, 100L to 10f)

        val first = ChartTransition.Rise.line(to, to)(0f)

        assertEquals(linkedMapOf(0L to 2f, 100L to 2f), first.values)
        assertEquals(to.copy(values = first.values), first)
    }

    @Test
    fun rise_growsFromZeroWhenTheRangeCrossesIt() {
        val to = frame(0, 100, -4f, 4f, 0L to -4f, 100L to 2f)
        val frameAt = ChartTransition.Rise.line(to, to)

        assertEquals(linkedMapOf(0L to 0f, 100L to 0f), frameAt(0f).values)
        assertEquals(linkedMapOf(0L to -2f, 100L to 1f), frameAt(0.5f).values)
        assertEquals(to, frameAt(1f))
    }

    @Test
    fun rise_dropsTheOldLine() {
        val to = frame(0, 200, 0f, 20f, 0L to 20f, 200L to 10f)

        assertEquals(linkedMapOf(0L to 10f, 200L to 5f), ChartTransition.Rise.line(day, to)(0.5f).values)
    }

    @Test
    fun fadeIn_keepsTheNewLineInPlaceAndRaisesItsOpacity() {
        val frameAt = ChartTransition.FadeIn().line(day.copy(alpha = 0f), day)

        assertEquals(day.copy(alpha = 0f), frameAt(0f))
        assertEquals(day.copy(alpha = 0.5f), frameAt(0.5f))
        assertEquals(day, frameAt(1f))
        // A spec that overshoots cannot make the line more than opaque.
        assertEquals(day, frameAt(1.2f))
    }

    @Test
    fun fadeOut_startsFromTheOpacityOnScreen() {
        val halfShown = day.copy(alpha = 0.5f)
        val frameAt = ChartTransition.FadeOut().line(halfShown, halfShown)

        assertEquals(halfShown, frameAt(0f))
        assertEquals(day.copy(alpha = 0.25f), frameAt(0.5f))
        assertEquals(day.copy(alpha = 0f), frameAt(1f))
    }

    @Test
    fun morph_bringsAHalfFadedLineToFullOpacity() {
        val to = frame(0, 200, 0f, 20f, 0L to 20f, 100L to 0f, 200L to 5f)
        val frameAt = ChartTransition.Morph.line(day.copy(alpha = 0.5f), to)

        assertEquals(0.5f, frameAt(0f).alpha)
        assertEquals(0.75f, frameAt(0.5f).alpha)
        assertEquals(1f, frameAt(1f).alpha)
    }

    @Test
    fun fades_finishAtTheFractionTheyAreGiven() {
        val fadeIn = ChartTransition.FadeIn(completeAt = 0.5f).line(day.copy(alpha = 0f), day)
        val fadeOut = ChartTransition.FadeOut(completeAt = 0.5f).line(day, day)

        assertEquals(0.5f, fadeIn(0.25f).alpha)
        assertEquals(1f, fadeIn(0.5f).alpha)
        assertEquals(1f, fadeIn(1f).alpha)

        assertEquals(0.5f, fadeOut(0.25f).alpha)
        assertEquals(0f, fadeOut(0.5f).alpha)
        assertEquals(0f, fadeOut(1f).alpha)
    }

    @Test
    fun fadeIn_startsFromTheOpacityOnScreen() {
        val frameAt = ChartTransition.FadeIn().line(day.copy(alpha = 0.5f), day)

        assertEquals(0.5f, frameAt(0f).alpha)
        assertEquals(0.75f, frameAt(0.5f).alpha)
        assertEquals(1f, frameAt(1f).alpha)
    }

    @Test
    fun morph_keepsAPeakOfTheOldLineThatFallsBetweenNewPoints() {
        val from = frame(0, 100, 0f, 10f, 0L to 2f, 50L to 10f, 100L to 2f)
        val to = frame(0, 200, 0f, 10f, 0L to 2f, 200L to 2f)
        val frameAt = ChartTransition.Morph.line(from, to)

        // The old peak was in the middle, where the new line has no point of its own.
        assertEquals(linkedMapOf(0L to 2f, 100L to 10f, 200L to 2f), LinkedHashMap(frameAt(0f).values))
        assertEquals(6f, frameAt(0.5f).values.getValue(100))
        assertEquals(to, frameAt(1f))
    }

    @Test
    fun morph_addsNoBarsForTheOldOnes() {
        val from = frame(0, 100, 0f, 10f, 0L to 2f, 50L to 10f, 100L to 2f)
        val to = frame(0, 200, 0f, 10f, 0L to 2f, 200L to 2f)

        assertEquals(listOf(0L, 200L), ChartTransition.Morph.bars(from, to)(0f).values.keys.toList())
    }
}
