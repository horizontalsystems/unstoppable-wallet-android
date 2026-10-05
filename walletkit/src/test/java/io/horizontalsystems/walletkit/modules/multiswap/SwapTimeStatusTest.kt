package io.horizontalsystems.walletkit.modules.multiswap

import org.junit.Assert.assertEquals
import org.junit.Test

class SwapTimeStatusTest {

    private fun minutes(value: Long) = value * 60

    private fun status(time: Long, vararg others: Long) =
        swapTimeStatus(minutes(time), (listOf(time) + others.toList()).map { minutes(it) })

    @Test
    fun underThreshold_isNone() {
        assertEquals(SwapTimeStatus.None, status(20, 2))
    }

    @Test
    fun displayedUpperBoundOverThreshold_isNone() {
        // CEX shows 21-35 min, but the quoted 28 min is what counts
        assertEquals(SwapTimeStatus.None, status(28, 2))
    }

    @Test
    fun displayedLowerBoundUnderThreshold_isAttention() {
        // CEX shows 24-40 min, but the quoted 32 min is what counts
        assertEquals(SwapTimeStatus.Attention, status(32, 2))
    }

    @Test
    fun closeToBaseline_isNone() {
        assertEquals(SwapTimeStatus.None, status(50, 40))
    }

    @Test
    fun exactlyTwiceBaseline_isAttention() {
        assertEquals(SwapTimeStatus.Attention, status(62, 31))
    }

    @Test
    fun exactlyThreshold_isNone() {
        assertEquals(SwapTimeStatus.None, status(30, 2))
    }

    @Test
    fun singleProviderOverThreshold_isAttention() {
        assertEquals(SwapTimeStatus.Attention, status(31))
    }

    @Test
    fun providersWithoutTime_areIgnoredForBaseline() {
        assertEquals(SwapTimeStatus.Attention, swapTimeStatus(minutes(31), listOf(minutes(31), null)))
    }

    @Test
    fun unknownTime_isNone() {
        assertEquals(SwapTimeStatus.None, swapTimeStatus(null, listOf(minutes(2))))
    }
}
