package io.horizontalsystems.walletkit.chain.zcash

import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.EndpointPingResult
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.EndpointPingResult.Status
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.ZcashEndpoint
import org.junit.Assert.assertEquals
import org.junit.Test

// A send goes on through the servers that answer a ping, fastest first
class ZcashSendServersTest {

    private val a = ZcashEndpoint("https://a.example:443", "a")
    private val b = ZcashEndpoint("https://b.example:443", "b")
    private val c = ZcashEndpoint("https://c.example:443", "c")

    private fun ok(endpoint: ZcashEndpoint, ms: Double) = EndpointPingResult(endpoint.url, Status.Ok, ms)

    @Test
    fun fastestFirst() {
        val pings = listOf(ok(a, 300.0), ok(b, 90.0), ok(c, 150.0))

        assertEquals(listOf(b, c, a), endpointsThatAnswer(listOf(a, b, c), pings))
    }

    @Test
    fun aServerThatDidNotAnswerIsLeftOut() {
        val failed = Status.values().first { it != Status.Ok }
        val pings = listOf(EndpointPingResult(a.url, failed, 10.0), ok(b, 90.0), ok(c, Double.MAX_VALUE))

        assertEquals(listOf(b), endpointsThatAnswer(listOf(a, b, c), pings))
    }

    @Test
    fun noneWhenNoPingCameBack() {
        assertEquals(emptyList<ZcashEndpoint>(), endpointsThatAnswer(listOf(a, b), emptyList()))
    }
}
