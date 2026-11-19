package io.horizontalsystems.chartview

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartTransitionTest {

    private fun frame(start: Long, end: Long, min: Float, max: Float, vararg points: Pair<Long, Float>) =
        CurveFrame(linkedMapOf(*points), start, end, min, max)

    private val day = frame(100, 200, 0f, 10f, 100L to 0f, 150L to 10f, 200L to 5f)

    @Test
    fun none_showsTheNewDataAtOnce() {
        val to = frame(0, 200, 0f, 20f, 0L to 20f, 200L to 0f)

        assertEquals(to, ChartTransition.None.line(day, to)(0f))
    }

    @Test
    fun moveAndScale_startsAtTheOldRangesAndEndsAtTheNewOnes() {
        val to = frame(0, 200, 0f, 20f, 0L to 20f, 100L to 0f, 200L to 5f)
        val frameAt = ChartTransition.MoveAndScale.line(day, to)

        val first = frameAt(0f)
        assertEquals(100, first.startTimestamp)
        assertEquals(200, first.endTimestamp)
        assertEquals(10f, first.maxValue)

        val half = frameAt(0.5f)
        assertEquals(50, half.startTimestamp)
        assertEquals(200, half.endTimestamp)
        assertEquals(15f, half.maxValue)

        val last = frameAt(1f)
        assertEquals(0, last.startTimestamp)
        assertEquals(20f, last.maxValue)
    }

    @Test
    fun moveAndScale_keepsTheOldLineInPlaceOnTheFirstFrame() {
        val to = frame(0, 200, 0f, 20f, 0L to 20f, 100L to 0f, 125L to 2f, 200L to 5f)

        val first = ChartTransition.MoveAndScale.line(day, to)(0f)

        // 125 lies between the old points 100 and 150, so it starts on the old line.
        assertEquals(5f, first.values.getValue(125))
        assertEquals(10f, first.values.getValue(150))
        // 0 is outside the old line, so it already holds its new value off screen.
        assertEquals(20f, first.values.getValue(0))
        assertEquals(listOf(0L, 100L, 125L, 150L, 200L), first.values.keys.toList())
    }

    @Test
    fun moveAndScale_keepsTimestampsExactAtUnixScale() {
        val from = frame(1_700_000_000, 1_700_086_400, 0f, 1f, 1_700_000_000L to 0f, 1_700_086_400L to 1f)
        val to = frame(1_700_000_100, 1_700_086_500, 0f, 1f, 1_700_000_100L to 0f, 1_700_086_500L to 1f)

        val half = ChartTransition.MoveAndScale.line(from, to)(0.5f)

        assertEquals(1_700_000_050, half.startTimestamp)
        assertEquals(1_700_086_450, half.endTimestamp)
    }

    @Test
    fun moveAndScale_bars_growMissingBarsFromAStub() {
        val from = frame(0, 20, 10f, 30f, 0L to 10f, 10L to 30f)
        val to = frame(0, 20, 20f, 40f, 10L to 40f, 20L to 20f)
        val frameAt = ChartTransition.MoveAndScale.bars(from, to)

        assertEquals(linkedMapOf(0L to 10f, 10L to 30f, 20L to 1f), frameAt(0f).values)
        assertEquals(linkedMapOf(0L to 6f, 10L to 35f, 20L to 10.5f), frameAt(0.5f).values)
    }

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
}
