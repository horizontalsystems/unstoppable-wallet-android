package io.horizontalsystems.walletkit.modules.multiswap.providers

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins how `GET /v3/providers` rows become venues, against a captured dev-server answer. The
 * fixture is trimmed to the fields the app reads; it is not a snapshot of live policy.
 */
class SwapProviderInfoManagerTest {

    private val rows: Map<String, UnstoppableAPI.Response.Provider> = Gson().fromJson(
        javaClass.classLoader!!.getResource("providers-dev.json")!!.readText(),
        Array<UnstoppableAPI.Response.Provider>::class.java,
    ).associateBy { it.provider.uppercase() }

    @Test
    fun `every synced row becomes a known venue with the server's name and policy`() {
        val venues = SwapProviderInfoManager.venues(rows)

        assertEquals(rows.keys, venues.keys)
        assertTrue(venues.values.all { it.isKnown })
        val near = venues.getValue("NEAR")
        assertEquals(rows.getValue("NEAR").name ?: "Near", near.name)
        assertEquals(rows.getValue("NEAR").amlPolicy?.let { RiskLevel.fromAmlPolicy(it) }, near.riskLevel)
        assertEquals(rows.getValue("NEAR").executionType, near.executionType)
    }

    @Test
    fun `privacy is read from the row, and absent means none`() {
        val venues = SwapProviderInfoManager.venues(rows)

        for ((id, row) in rows) {
            assertEquals(id, row.isConfidential, venues.getValue(id).isConfidential)
        }
        assertFalse(venues.getValue("NEAR").isConfidential)
    }

    @Test
    fun `split venues are the rows that serve splits and are not suspended`() {
        val suspendedSplit = UnstoppableAPI.Response.Provider(provider = "SPLIT_OFF", splitSwaps = true, suspended = true)
        val liveSplit = UnstoppableAPI.Response.Provider(provider = "SPLIT_ON", splitSwaps = true, suspended = false)
        val withSplits = rows + mapOf("SPLIT_OFF" to suspendedSplit, "SPLIT_ON" to liveSplit)

        val ids = SwapProviderInfoManager.splitProviderIds(withSplits)

        assertTrue("SPLIT_ON" in ids)
        assertFalse("SPLIT_OFF" in ids)
    }

    @Test
    fun `a row with no name is humanized`() {
        val row = UnstoppableAPI.Response.Provider(provider = "PANCAKESWAP_API", name = null)

        assertEquals("Pancakeswap Api", SwapProviderInfoManager.venues(mapOf("PANCAKESWAP_API" to row)).getValue("PANCAKESWAP_API").name)
    }
}
