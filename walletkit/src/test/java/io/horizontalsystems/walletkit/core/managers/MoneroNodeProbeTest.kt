package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.managers.MoneroNodeManager.MoneroNode
import io.horizontalsystems.walletkit.core.managers.MoneroNodeManager.NodePingResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

// Auto-Select picks the fastest Monero node among those that are not behind the others
class MoneroNodeProbeTest {

    private val a = node("a")
    private val b = node("b")
    private val c = node("c")
    private val top = 3_815_000L

    @Test
    fun aNodeThatAnsweredHasItsTimeAndHeight() {
        val probe = MoneroNodeManager.probes(listOf(a), listOf(ping(a, 120.0, top))).single()

        assertEquals(SourceProbe(a, 120.0, top), probe)
    }

    @Test
    fun aNodeThatDidNotAnswerIsNotHealthy() {
        val probes = MoneroNodeManager.probes(
            nodes = listOf(a, b, c),
            results = listOf(
                NodePingResult(a.serialized, isValid = false, responseTime = 80.0, height = top),
                NodePingResult(b.serialized, isValid = true, responseTime = Double.MAX_VALUE, height = 0),
            ),
        )

        assertEquals(3, probes.size)
        probes.forEach { assertFalse(it.isHealthy) }
    }

    @Test
    fun theFastestNodeIsPicked() {
        val results = listOf(ping(a, 300.0, top), ping(b, 90.0, top), ping(c, 150.0, top - 1))

        assertEquals(b, MoneroNodeManager.fastestNode(listOf(a, b, c), results))
    }

    @Test
    fun aFasterNodeThatIsBehindIsLeftOut() {
        val behind = top - MoneroNodeManager.LAG_THRESHOLD - 1
        val results = listOf(ping(a, 300.0, top), ping(b, 90.0, behind))

        assertEquals(a, MoneroNodeManager.fastestNode(listOf(a, b), results))
    }

    @Test
    fun aNodeAFewBlocksBackStillCounts() {
        val results = listOf(ping(a, 300.0, top), ping(b, 90.0, top - MoneroNodeManager.LAG_THRESHOLD))

        assertEquals(b, MoneroNodeManager.fastestNode(listOf(a, b), results))
    }

    @Test
    fun nothingIsPickedWhenNoNodeAnswered() {
        assertNull(MoneroNodeManager.fastestNode(listOf(a, b), emptyList()))
    }

    private fun node(name: String) = MoneroNode("$name.example:18089", name, "$name.example:18089/mainnet/$name")

    private fun ping(node: MoneroNode, responseTime: Double, height: Long) =
        NodePingResult(node.serialized, isValid = true, responseTime = responseTime, height = height)
}
