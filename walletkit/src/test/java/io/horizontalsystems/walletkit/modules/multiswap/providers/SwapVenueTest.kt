package io.horizontalsystems.walletkit.modules.multiswap.providers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwapVenueTest {

    @Test
    fun `an unknown id gets a humanized name and no policy`() {
        val venue = SwapVenue.unknown("PANCAKESWAP_API")

        assertEquals("Pancakeswap Api", venue.name)
        assertNull(venue.riskLevel)
        assertFalse(venue.isConfidential)
        assertFalse(venue.isKnown)
    }

    @Test
    fun `any privacy level other than none is confidential`() {
        assertTrue(SwapVenue.unknown("X").copy(privacy = "basic").isConfidential)
        assertTrue(SwapVenue.unknown("X").copy(privacy = "advanced").isConfidential)
        assertFalse(SwapVenue.unknown("X").copy(privacy = "none").isConfidential)
    }

    @Test
    fun `the route's aml policy wins over the synced one, and an unknown value changes nothing`() {
        val venue = SwapVenue.unknown("X").copy(riskLevel = RiskLevel.GOOD)

        assertEquals(RiskLevel.FAIR, venue.overriding("fair").riskLevel)
        assertEquals(RiskLevel.EXCELLENT, venue.overriding("EXCELLENT").riskLevel)
        assertEquals(RiskLevel.GOOD, venue.overriding("whatever").riskLevel)
        assertEquals(RiskLevel.GOOD, venue.overriding(null).riskLevel)
    }
}
