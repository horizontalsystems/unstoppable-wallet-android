package io.horizontalsystems.walletkit.chain.zano

import io.horizontalsystems.walletkit.core.managers.ZanoNodeManager.ZanoNode
import io.horizontalsystems.zanokit.NodePingResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZanoNodeProbeTest {

    private val node = ZanoNode("https://node.zano.org:443", "node.zano.org")

    @Test
    fun validPingIsAHealthyProbeWithItsTimeAndHeight() {
        val probe = nodeProbe(node, NodePingResult(node.host, responseTime = 120.0, height = 3908750, isValid = true))

        assertTrue(probe.isHealthy)
        assertEquals(120.0, probe.responseTimeMs!!, 0.0)
        assertEquals(3908750L, probe.height)
    }

    @Test
    fun pingThatIsNotValidIsAFailedProbe() {
        val probe = nodeProbe(node, NodePingResult(node.host, responseTime = Double.MAX_VALUE, height = 0, isValid = false))

        assertFalse(probe.isHealthy)
    }

    // A daemon that answered but is still downloading blocks has a time and no usable height
    @Test
    fun daemonThatAnsweredButIsNotReadyIsAFailedProbe() {
        val probe = nodeProbe(node, NodePingResult(node.host, responseTime = 80.0, height = 0, isValid = false))

        assertFalse(probe.isHealthy)
    }

    @Test
    fun nodeTheKitReturnedNothingForIsAFailedProbe() {
        assertFalse(nodeProbe(node, null).isHealthy)
    }
}
