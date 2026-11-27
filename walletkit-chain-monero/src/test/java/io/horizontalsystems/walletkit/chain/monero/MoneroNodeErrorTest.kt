package io.horizontalsystems.walletkit.chain.monero

import io.horizontalsystems.monerokit.SendResult
import io.horizontalsystems.walletkit.core.adapters.MoneroNotSentException
import io.horizontalsystems.walletkit.core.adapters.MoneroSendUnknownException
import io.horizontalsystems.walletkit.core.chain.NodeError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneroNodeErrorTest {

    private val plugin = MoneroChainPlugin(
        { error("Context is not needed to classify an error") },
        { error("MoneroNodeManager is not needed to classify an error") },
    )

    // No node the kit could ask confirmed the transfer, so sending again could pay twice
    @Test
    fun transferNoNodeConfirmedMayHaveBeenSent() {
        assertEquals(
            NodeError(preBroadcast = false),
            plugin.classifyNodeError(
                MoneroSendUnknownException("aa11", "Send Transaction failed: no connection to daemon. Please make sure daemon is running.")
            ),
        )
    }

    // The kit has already tried another node where it may; the screen must not move the chain again
    @Test
    fun transferThatWasNotSentIsNotANodeFailureForTheScreen() {
        SendResult.Reason.values().forEach {
            assertNull(it.name, plugin.classifyNodeError(MoneroNotSentException(it, "not sent")))
        }
    }

    // A fee estimate the kit could not get from any node it may use: it has already tried
    @Test
    fun failedFeeEstimateIsNotANodeFailureForTheScreen() {
        assertNull(plugin.classifyNodeError(IllegalStateException("internal error: Failed to get fee estimate")))
    }
}
