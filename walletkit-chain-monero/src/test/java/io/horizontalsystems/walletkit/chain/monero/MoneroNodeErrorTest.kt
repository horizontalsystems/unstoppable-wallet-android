package io.horizontalsystems.walletkit.chain.monero

import io.horizontalsystems.walletkit.core.chain.NodeError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// The messages are the ones monero-kit's WalletService raises
class MoneroNodeErrorTest {

    private fun classify(message: String?) = moneroNodeError(IllegalStateException(message))

    @Test
    fun unreachableNodeWhileBuildingIsBeforeTheBroadcast() {
        val error = "Create Transaction failed: node is unreachable (internal error: Failed to get earliest fork height)"

        assertEquals(NodeError(preBroadcast = true), classify(error))
    }

    @Test
    fun nodeThatStoppedAnsweringWhileBuildingIsBeforeTheBroadcast() {
        val error = "Create Transaction failed: node is not responding (no connection to daemon)"

        assertEquals(NodeError(preBroadcast = true), classify(error))
    }

    @Test
    fun feeEstimateTheDaemonFailedIsBeforeTheBroadcast() {
        assertEquals(NodeError(preBroadcast = true), classify("internal error: Failed to get fee estimate"))
    }

    // The commit is the broadcast: the daemon may have taken the transaction and lost the answer
    @Test
    fun daemonFailureWhileCommittingMayHaveSent() {
        assertEquals(NodeError(preBroadcast = false), classify("Send Transaction failed: no connection to daemon"))
    }

    @Test
    fun transactionTheDaemonRefusedIsNotANodeFailure() {
        assertNull(classify("Send Transaction failed: transaction was rejected by daemon: double spend"))
    }

    @Test
    fun transactionThatCannotBeBuiltIsNotANodeFailure() {
        assertNull(classify("Create Transaction failed: not enough money to transfer, available only 0.1"))
        assertNull(classify("Create Transaction failed: Wallet is NULL"))
        assertNull(classify(null))
    }
}
