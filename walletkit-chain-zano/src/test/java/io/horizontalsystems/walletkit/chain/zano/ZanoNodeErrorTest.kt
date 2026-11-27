package io.horizontalsystems.walletkit.chain.zano

import io.horizontalsystems.walletkit.core.adapters.ZanoNotSentException
import io.horizontalsystems.walletkit.core.adapters.ZanoSendUnknownException
import io.horizontalsystems.walletkit.core.chain.NodeError
import io.horizontalsystems.zanokit.SendResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZanoNodeErrorTest {

    private val plugin = ZanoChainPlugin(
        { error("ZanoNodeManager is not needed to classify an error") },
        { error("BackgroundManager is not needed to classify an error") },
    )

    // No node the kit could ask confirmed the transfer, so sending again could pay twice
    @Test
    fun transferNoNodeConfirmedMayHaveBeenSent() {
        assertEquals(
            NodeError(preBroadcast = false),
            plugin.classifyNodeError(ZanoSendUnknownException("c7475d1c", "no answer to the broadcast")),
        )
    }

    // The kit has already tried another node where it may; the screen must not move the chain again
    @Test
    fun transferThatWasNotSentIsNotANodeFailureForTheScreen() {
        SendResult.Reason.values().forEach {
            assertNull(it.name, plugin.classifyNodeError(ZanoNotSentException(it, "not sent")))
        }
    }
}
