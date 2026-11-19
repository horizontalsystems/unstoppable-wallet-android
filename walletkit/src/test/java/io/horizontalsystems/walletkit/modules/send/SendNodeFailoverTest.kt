package io.horizontalsystems.walletkit.modules.send

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.chain.NodeError
import io.horizontalsystems.walletkit.modules.send.SendNodeFailover.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class SendNodeFailoverTest {

    private val monero = Token(Coin("monero", "Monero", "XMR"), Blockchain(BlockchainType.Monero, "Monero", null), TokenType.Native, 12)

    private class BeforeBroadcast : Exception("node is unreachable")
    private class MaybeBroadcast : Exception("no answer to the broadcast")

    private var otherNode: String? = "node-b"
    private var switches = 0
    private var onSwitch: () -> Unit = {}

    // The background switch runs right away, so a test sees it
    private val failover = SendNodeFailover(
        token = monero,
        classify = {
            when (it) {
                is BeforeBroadcast -> NodeError(preBroadcast = true)
                is MaybeBroadcast -> NodeError(preBroadcast = false)
                else -> null
            }
        },
        switchNode = {
            switches++
            onSwitch()
            otherNode
        },
        backgroundScope = CoroutineScope(Dispatchers.Unconfined),
    )

    @Test
    fun errorThatIsNotTheNodesIsLeftAlone() = runBlocking {
        assertEquals(Outcome.Unhandled, failover.handle(IOException("not enough money")))
        assertEquals(0, switches)
    }

    @Test
    fun nodeFailingBeforeTheBroadcastMovesTheChain() = runBlocking {
        assertEquals(Outcome.Switched("node-b"), failover.handle(BeforeBroadcast()))
        assertEquals(1, switches)
    }

    @Test
    fun errorStandsWhenThereIsNoOtherNode() = runBlocking {
        otherNode = null

        assertEquals(Outcome.Unhandled, failover.handle(BeforeBroadcast()))
    }

    // A second node failing too is the user's to see, not a reason to keep hopping
    @Test
    fun chainIsMovedAtMostOncePerScreen() = runBlocking {
        failover.handle(BeforeBroadcast())

        assertEquals(Outcome.Unhandled, failover.handle(BeforeBroadcast()))
        assertEquals(1, switches)
    }

    // Sending again could pay twice, so the screen is only told; the node is still changed so
    // that the wallet syncs and shows what happened
    @Test
    fun sendThatMayHaveGoneOutIsNotRetried() = runBlocking {
        assertEquals(Outcome.MaybeSent, failover.handle(MaybeBroadcast()))
        assertEquals(1, switches)
    }

    @Test
    fun sendThatMayHaveGoneOutIsReportedEvenWithoutAnotherNode() = runBlocking {
        otherNode = null

        assertEquals(Outcome.MaybeSent, failover.handle(MaybeBroadcast()))
        assertEquals(Outcome.MaybeSent, failover.handle(MaybeBroadcast()))
        assertEquals(1, switches)
    }

    @Test
    fun failedSwitchLeavesTheErrorAsItIs() = runBlocking {
        onSwitch = { throw IllegalStateException("adapter is null") }

        assertEquals(Outcome.Unhandled, failover.handle(BeforeBroadcast()))
    }

    @Test
    fun failedBackgroundSwitchStillReportsTheSendAsUncertain() = runBlocking {
        onSwitch = { throw IOException("no network") }

        assertEquals(Outcome.MaybeSent, failover.handle(MaybeBroadcast()))
    }

    // The user left the screen while the chain was being moved
    @Test
    fun cancellationIsNotSwallowed() = runBlocking {
        onSwitch = { throw CancellationException("screen closed") }

        try {
            failover.handle(BeforeBroadcast())
            fail("expected the cancellation")
        } catch (e: CancellationException) {
            assertEquals("screen closed", e.message)
        }
    }
}
