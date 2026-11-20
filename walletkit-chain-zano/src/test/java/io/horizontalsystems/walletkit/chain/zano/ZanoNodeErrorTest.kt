package io.horizontalsystems.walletkit.chain.zano

import io.horizontalsystems.walletkit.core.chain.NodeError
import io.horizontalsystems.zanokit.BroadcastUnconfirmedException
import io.horizontalsystems.zanokit.InsufficientFundsException
import io.horizontalsystems.zanokit.NodeUnreachableException
import io.horizontalsystems.zanokit.SendFailedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZanoNodeErrorTest {

    private val plugin = ZanoChainPlugin(
        { error("ZanoNodeManager is not needed to classify an error") },
        { error("BackgroundManager is not needed to classify an error") },
    )

    // The kit raises it for a daemon call that failed while the transfer was being built
    @Test
    fun unreachableNodeIsBeforeTheBroadcast() {
        assertEquals(NodeError(preBroadcast = true), plugin.classifyNodeError(NodeUnreachableException("no connection to daemon")))
    }

    // The daemon may have taken the transaction, so sending again could pay twice
    @Test
    fun broadcastWithoutAnAnswerMayHaveBeenSent() {
        assertEquals(
            NodeError(preBroadcast = false),
            plugin.classifyNodeError(BroadcastUnconfirmedException("no connection to daemon, request = sendrawtransaction")),
        )
    }

    @Test
    fun otherSendErrorsAreNotNodeFailures() {
        assertNull(plugin.classifyNodeError(InsufficientFundsException("NOT_ENOUGH_MONEY")))
        assertNull(plugin.classifyNodeError(SendFailedException("transfer failed")))
    }
}
