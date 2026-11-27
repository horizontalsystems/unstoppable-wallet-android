package io.horizontalsystems.walletkit.core.managers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceRepickTest {

    private fun probe(source: String, ms: Double?, height: Long?) = SourceProbe(source, ms, height)

    @Test
    fun staysWhenCurrentIsFastest() {
        val probes = listOf(probe("a", 100.0, 1000), probe("b", 200.0, 1000))

        assertNull(SourceRepick.choose("a", probes, lagThreshold = 5))
    }

    @Test
    fun staysWhenOnlySlightlySlower() {
        val probes = listOf(probe("a", 500.0, 1000), probe("b", 300.0, 1000))

        assertNull(SourceRepick.choose("a", probes, lagThreshold = 5))
    }

    @Test
    fun staysWhenTwiceAsSlowButGainIsSmall() {
        val probes = listOf(probe("a", 250.0, 1000), probe("b", 100.0, 1000))

        assertNull(SourceRepick.choose("a", probes, lagThreshold = 5))
    }

    @Test
    fun switchesWhenClearlySlower() {
        val probes = listOf(probe("a", 1200.0, 1000), probe("b", 300.0, 1000))

        assertEquals("b", SourceRepick.choose("a", probes, lagThreshold = 5))
    }

    @Test
    fun switchesWhenCurrentFailed() {
        val probes = listOf(probe("a", null, null), probe("b", 900.0, 1000), probe("c", 400.0, 1000))

        assertEquals("c", SourceRepick.choose("a", probes, lagThreshold = 5))
    }

    @Test
    fun switchesWhenCurrentLags() {
        val probes = listOf(probe("a", 50.0, 990), probe("b", 900.0, 1000))

        assertEquals("b", SourceRepick.choose("a", probes, lagThreshold = 5))
    }

    @Test
    fun lagWithinThresholdIsFresh() {
        val probes = listOf(probe("a", 50.0, 996), probe("b", 900.0, 1000))

        assertNull(SourceRepick.choose("a", probes, lagThreshold = 5))
    }

    @Test
    fun laggingSourceIsNeverPicked() {
        val probes = listOf(probe("a", null, null), probe("b", 10.0, 900), probe("c", 800.0, 1000))

        assertEquals("c", SourceRepick.choose("a", probes, lagThreshold = 5))
    }

    @Test
    fun staysWhenEverythingFailed() {
        val probes = listOf(probe("a", null, null), probe("b", null, null))

        assertNull(SourceRepick.choose("a", probes, lagThreshold = 5))
    }

    // A chain whose kit is costly to rebuild leaves its source only when it failed or lags

    @Test
    fun withoutSpeedSwitchingStaysOnAClearlySlowerSource() {
        val probes = listOf(probe("a", 3000.0, 1000), probe("b", 100.0, 1000))

        assertNull(SourceRepick.choose("a", probes, lagThreshold = 5, switchForSpeed = false))
    }

    @Test
    fun withoutSpeedSwitchingStillLeavesAFailedOrLaggingSource() {
        val failed = listOf(probe("a", null, null), probe("b", 900.0, 1000))
        val lagging = listOf(probe("a", 50.0, 900), probe("b", 900.0, 1000))

        assertEquals("b", SourceRepick.choose("a", failed, lagThreshold = 5, switchForSpeed = false))
        assertEquals("b", SourceRepick.choose("a", lagging, lagThreshold = 5, switchForSpeed = false))
    }

    @Test
    fun fastestSkipsFailedAndLaggingSources() {
        val probes = listOf(probe("a", null, null), probe("b", 10.0, 900), probe("c", 800.0, 1000), probe("d", 400.0, 998))

        assertEquals("d", SourceRepick.fastest(probes, lagThreshold = 5))
        assertNull(SourceRepick.fastest(listOf(probe("a", null, null)), lagThreshold = 5))
    }
}
