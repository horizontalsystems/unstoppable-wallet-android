package cash.p.terminal.modules.multiswap.providers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderEtaTest {

    // --- parseMinutesRangeToSeconds (ChangeNow) ---

    @Test
    fun parseMinutesRangeToSeconds_range_returnsUpperBoundInSeconds() {
        assertEquals(3600L, parseMinutesRangeToSeconds("10-60"))
    }

    @Test
    fun parseMinutesRangeToSeconds_singleNumber_returnsThatNumberInSeconds() {
        assertEquals(1800L, parseMinutesRangeToSeconds("30"))
    }

    @Test
    fun parseMinutesRangeToSeconds_numberWithExtraText_extractsNumber() {
        assertEquals(300L, parseMinutesRangeToSeconds("~5 min"))
    }

    @Test
    fun parseMinutesRangeToSeconds_noDigits_returnsNull() {
        assertNull(parseMinutesRangeToSeconds("fast"))
    }

    @Test
    fun parseMinutesRangeToSeconds_empty_returnsNull() {
        assertNull(parseMinutesRangeToSeconds(""))
    }

    @Test
    fun parseMinutesRangeToSeconds_null_returnsNull() {
        assertNull(parseMinutesRangeToSeconds(null))
    }

    // --- resolveAllBridgeTransferTimeSeconds (AllBridge) ---

    @Test
    fun resolveAllBridgeTransferTimeSeconds_officialNamedKey_resolvesMsToSeconds() {
        val transferTime = mapOf("ARB" to mapOf("allbridge" to 1_020_000L))

        assertEquals(
            1020L,
            resolveAllBridgeTransferTimeSeconds(transferTime, "ARB", crossChain = true)
        )
    }

    @Test
    fun resolveAllBridgeTransferTimeSeconds_proxyNumericKey_resolvesMsToSeconds() {
        val transferTime = mapOf("ARB" to mapOf("1" to 1_020_000L))

        assertEquals(
            1020L,
            resolveAllBridgeTransferTimeSeconds(transferTime, "ARB", crossChain = true)
        )
    }

    @Test
    fun resolveAllBridgeTransferTimeSeconds_messengerValueNull_returnsNull() {
        val transferTime = mapOf("ARB" to mapOf("allbridge" to null))

        assertNull(resolveAllBridgeTransferTimeSeconds(transferTime, "ARB", crossChain = true))
    }

    @Test
    fun resolveAllBridgeTransferTimeSeconds_destinationChainAbsent_returnsNull() {
        val transferTime = mapOf("BSC" to mapOf("allbridge" to 1_020_000L))

        assertNull(resolveAllBridgeTransferTimeSeconds(transferTime, "ARB", crossChain = true))
    }

    @Test
    fun resolveAllBridgeTransferTimeSeconds_sameChain_returnsNull() {
        val transferTime = mapOf("ARB" to mapOf("allbridge" to 1_020_000L))

        assertNull(resolveAllBridgeTransferTimeSeconds(transferTime, "ARB", crossChain = false))
    }

    @Test
    fun resolveAllBridgeTransferTimeSeconds_allbridgeNull_doesNotFallBackToOtherMessenger() {
        val transferTime = mapOf(
            "ARB" to mapOf("allbridge" to null, "wormhole" to 1_080_000L, "cctp" to 1_140_000L)
        )

        assertNull(resolveAllBridgeTransferTimeSeconds(transferTime, "ARB", crossChain = true))
    }

    @Test
    fun resolveAllBridgeTransferTimeSeconds_transferTimeNull_returnsNull() {
        assertNull(resolveAllBridgeTransferTimeSeconds(null, "ARB", crossChain = true))
    }
}
