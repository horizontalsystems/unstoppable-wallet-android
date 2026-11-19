package io.horizontalsystems.walletkit.chain.zcash

import cash.z.ecc.android.sdk.model.ZcashNetwork
import io.horizontalsystems.walletkit.chain.zcash.ZcashEndpointPinger.Probe
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.EndpointPingResult.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZcashEndpointPingerTest {

    private fun statuses(vararg probes: Pair<String, Probe>) =
        ZcashEndpointPinger.results(probes.toList()).associate { it.url to it.status }

    @Test
    fun syncedServer_isOkWithItsResponseTime() {
        val result = ZcashEndpointPinger.results(listOf("a" to Probe.Synced(120.0, 3_000_000))).single()

        assertEquals(Status.Ok, result.status)
        assertTrue(result.isValid)
        assertEquals(120.0, result.responseTime, 0.0)
    }

    @Test
    fun serverThatDidNotAnswer_isUnreachable() {
        val result = ZcashEndpointPinger.results(listOf("a" to Probe.Unreachable)).single()

        assertEquals(Status.Unreachable, result.status)
        assertFalse(result.isValid)
        assertEquals(Double.MAX_VALUE, result.responseTime, 0.0)
    }

    @Test
    fun serverStillCatchingUp_isNotSynced() {
        val result = ZcashEndpointPinger.results(listOf("a" to Probe.NotSynced)).single()

        assertEquals(Status.NotSynced, result.status)
        assertFalse(result.isValid)
        assertEquals(Double.MAX_VALUE, result.responseTime, 0.0)
    }

    @Test
    fun serverWithinTheLagThreshold_isOk() {
        val statuses = statuses(
            "best" to Probe.Synced(100.0, 3_000_000),
            "close" to Probe.Synced(100.0, 2_999_995),
        )

        assertEquals(Status.Ok, statuses["best"])
        assertEquals(Status.Ok, statuses["close"])
    }

    @Test
    fun serverBehindTheOthers_isNotSynced() {
        val warnings = mutableListOf<String>()
        val results = ZcashEndpointPinger.results(
            listOf(
                "best" to Probe.Synced(100.0, 3_000_000),
                "stale" to Probe.Synced(50.0, 2_999_994),
            ),
            warn = warnings::add,
        ).associateBy { it.url }

        assertEquals(Status.Ok, results.getValue("best").status)
        assertEquals(Status.NotSynced, results.getValue("stale").status)
        assertEquals(Double.MAX_VALUE, results.getValue("stale").responseTime, 0.0)
        assertEquals(1, warnings.size)
    }

    @Test
    fun onlyServerThatAnswered_isOkWhateverItsHeight() {
        val statuses = statuses(
            "local" to Probe.Synced(10.0, 100),
            "down" to Probe.Unreachable,
            "syncing" to Probe.NotSynced,
        )

        assertEquals(Status.Ok, statuses["local"])
        assertEquals(Status.Unreachable, statuses["down"])
        assertEquals(Status.NotSynced, statuses["syncing"])
    }

    private val sapling = ZcashNetwork.Mainnet.saplingActivationHeight.value

    private fun verdict(
        chainMatches: Boolean = true,
        saplingActivationHeight: Long = sapling,
        blockHeight: Long = 3_000_000,
        estimatedHeight: Long = 3_000_000,
    ) = ZcashEndpointPinger.serverInfoVerdict(chainMatches, saplingActivationHeight, blockHeight, estimatedHeight)

    @Test
    fun serverNineBlocksBelowItsEstimatedTip_goesOnToTheHeightRequest() {
        assertNull(verdict(estimatedHeight = 3_000_009))
        assertNull(verdict(estimatedHeight = 3_000_000))
    }

    @Test
    fun serverTenBlocksBelowItsEstimatedTip_isNotSynced() {
        assertEquals(Probe.NotSynced, verdict(estimatedHeight = 3_000_010)?.probe)
        assertEquals(Probe.NotSynced, verdict(blockHeight = 100)?.probe)
    }

    @Test
    fun serverOfAnotherNetwork_isUnreachableEvenWhenCatchingUp() {
        assertEquals(Probe.Unreachable, verdict(chainMatches = false)?.probe)
        assertEquals(Probe.Unreachable, verdict(chainMatches = false, estimatedHeight = 3_000_010)?.probe)
        assertEquals(Probe.Unreachable, verdict(saplingActivationHeight = sapling + 1)?.probe)
    }

    @Test
    fun eachRejection_namesTheCheckThatFailed() {
        assertEquals("serves another chain", verdict(chainMatches = false)?.reason)
        assertEquals("sapling activation height is not mainnet's", verdict(saplingActivationHeight = 0)?.reason)
        assertEquals("still catching up with the chain", verdict(estimatedHeight = 3_000_010)?.reason)
        // The chain is checked first, so it is the one named when several checks fail.
        assertEquals(
            "serves another chain",
            verdict(chainMatches = false, saplingActivationHeight = 0, estimatedHeight = 3_000_010)?.reason,
        )
    }

    @Test
    fun label_isHostAndPortWithoutCredentials() {
        assertEquals("zec.rocks:443", ZcashEndpointPinger.label("https://zec.rocks"))
        assertEquals("zec.rocks:9067", ZcashEndpointPinger.label("https://user:secret@zec.rocks:9067"))
        assertEquals("invalid url", ZcashEndpointPinger.label("user:secret@not a url"))
    }

    @Test
    fun warningAboutAStaleServer_doesNotCarryCredentials() {
        val warnings = mutableListOf<String>()
        ZcashEndpointPinger.results(
            listOf(
                "https://zec.rocks:443" to Probe.Synced(100.0, 3_000_000),
                "https://user:secret@stale.example.com:9067" to Probe.Synced(50.0, 2_999_000),
            ),
            warn = warnings::add,
        )

        assertTrue(warnings.single().startsWith("stale.example.com:9067:"))
        assertFalse(warnings.single().contains("secret"))
    }
}
