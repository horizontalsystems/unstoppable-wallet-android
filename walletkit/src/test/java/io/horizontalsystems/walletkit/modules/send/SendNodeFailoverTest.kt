package io.horizontalsystems.walletkit.modules.send

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.chain.NodeError
import io.horizontalsystems.walletkit.core.chain.NodeSwitchUnfinishedException
import io.horizontalsystems.walletkit.modules.send.SendNodeFailover.Delivery
import io.horizontalsystems.walletkit.modules.send.SendNodeFailover.Outcome
import io.horizontalsystems.walletkit.modules.send.SendNodeFailover.SentTransfer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

class SendNodeFailoverTest {

    private val monero = Token(Coin("monero", "Monero", "XMR"), Blockchain(BlockchainType.Monero, "Monero", null), TokenType.Native, 12)

    private class BeforeBroadcast : Exception("node is unreachable")
    private class MaybeBroadcast : Exception("no answer to the broadcast")

    private var otherNode: String? = "node-b"
    private var switches = 0
    private var onSwitch: () -> Unit = {}

    // What the wallet lists as sent: one list before the node is changed, one after
    private val sendStartedAt = 1_000_000_000L
    private val earlier = SentTransfer("earlier", BigDecimal("2"), sendStartedAt / 1000 - 3_600)
    private val theSend = SentTransfer("the-send", BigDecimal("1.5"), sendStartedAt / 1000 + 5)
    private var sentBefore: List<SentTransfer> = listOf(earlier)
    private var sentAfter: List<SentTransfer> = listOf(earlier)
    private var listFails = false
    private var time = sendStartedAt

    // The adapter the chain runs on; a node change replaces it
    private var adapter: Any? = "adapter on node-a"

    // The switch runs right away, so a test sees it
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
        sentTransfers = {
            if (listFails) throw IOException("wallet is busy")
            if (switches == 0) sentBefore else sentAfter
        },
        lookupTimeMs = 3_000,
        lookupIntervalMs = 1_000,
        now = { time },
        wait = { time += it },
        currentAdapter = { adapter },
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

    // The node is changed, and the adapter on the new one is not there when the wait ends.
    // The screen's send service holds the old adapter until the new one has come.
    @Test
    fun switchThatFinishesLateIsHandedOverOnceTheAdapterIsThere() = runBlocking {
        onSwitch = { throw NodeSwitchUnfinishedException("node-b") }

        assertEquals(Outcome.Unhandled, failover.handle(BeforeBroadcast()))
        assertEquals(null, failover.lateSwitch())

        adapter = null // the old one is stopped, the new one is still being built
        assertEquals(null, failover.lateSwitch())

        adapter = "adapter on node-b"
        assertEquals(Outcome.Switched("node-b"), failover.lateSwitch())
        // given once
        assertEquals(null, failover.lateSwitch())
    }

    @Test
    fun noLateSwitchAfterOneThatFinishedInTime() = runBlocking {
        onSwitch = { adapter = "adapter on node-b" }

        assertEquals(Outcome.Switched("node-b"), failover.handle(BeforeBroadcast()))
        assertEquals(null, failover.lateSwitch())
    }

    @Test
    fun noLateSwitchWhenThereWasNoOtherNode() = runBlocking {
        otherNode = null
        failover.handle(BeforeBroadcast())

        adapter = "an adapter rebuilt for another reason"
        assertEquals(null, failover.lateSwitch())
    }

    // A second node failing too is the user's to see, not a reason to keep hopping
    @Test
    fun chainIsMovedAtMostOncePerScreen() = runBlocking {
        failover.handle(BeforeBroadcast())

        assertEquals(Outcome.Unhandled, failover.handle(BeforeBroadcast()))
        assertEquals(1, switches)
    }

    // Sending again could pay twice, so the screen is only told. The node is changed when the
    // screen asks what became of the send.
    @Test
    fun sendThatMayHaveGoneOutIsNotRetried() = runBlocking {
        assertEquals(Outcome.MaybeSent, failover.handle(MaybeBroadcast()))
        assertEquals(0, switches)
    }

    @Test
    fun transferListedOnTheNewNodeIsFound() = runBlocking {
        sentAfter = listOf(theSend, earlier)

        assertEquals(Delivery.Found("the-send"), failover.confirmDelivery(BigDecimal("1.50"), sendStartedAt))
        assertEquals(1, switches)
    }

    @Test
    fun transferThatAppearsWhileLookingIsFound() = runBlocking {
        var reads = 0
        val failover = SendNodeFailover(
            token = monero,
            classify = { null },
            switchNode = { "node-b" },
            backgroundScope = CoroutineScope(Dispatchers.Unconfined),
            sentTransfers = { if (reads++ < 3) listOf(earlier) else listOf(theSend, earlier) },
            lookupTimeMs = 3_000,
            lookupIntervalMs = 1_000,
            now = { time },
            wait = { time += it },
        )

        assertEquals(Delivery.Found("the-send"), failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt))
    }

    @Test
    fun transferThatIsNotListedIsNotFoundAndNamesTheNewNode() = runBlocking {
        assertEquals(Delivery.NotFound("node-b"), failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt))
        assertEquals(1, switches)
    }

    @Test
    fun transferListedBeforeTheNodeChangeIsNotTakenForTheSend() = runBlocking {
        sentBefore = listOf(theSend, earlier)
        sentAfter = listOf(theSend, earlier)

        assertEquals(Delivery.NotFound("node-b"), failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt))
    }

    @Test
    fun transferOfAnotherAmountIsNotTakenForTheSend() = runBlocking {
        sentAfter = listOf(theSend.copy(amount = BigDecimal("1.49")), earlier)

        assertEquals(Delivery.NotFound("node-b"), failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt))
    }

    // A wallet that did not record the send itself may list it with the amount of its change
    @Test
    fun transferToSelfIsFoundWhateverAmountTheWalletLists() = runBlocking {
        sentAfter = listOf(theSend.copy(amount = BigDecimal("1.5099"), toSelf = true), earlier)

        assertEquals(
            Delivery.Found("the-send"),
            failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt, toSelf = true),
        )
    }

    @Test
    fun transferToSelfOfAnotherAmountIsNotTakenForASendToSomeoneElse() = runBlocking {
        sentAfter = listOf(theSend.copy(amount = BigDecimal("1.5099"), toSelf = true), earlier)

        assertEquals(Delivery.NotFound("node-b"), failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt))
    }

    @Test
    fun transferToSomeoneElseOfAnotherAmountIsNotTakenForASendToSelf() = runBlocking {
        sentAfter = listOf(theSend.copy(amount = BigDecimal("1.5099")), earlier)

        assertEquals(
            Delivery.NotFound("node-b"),
            failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt, toSelf = true),
        )
    }

    // An old transfer the wallet only now lists, as after a resync
    @Test
    fun transferFromLongBeforeTheSendIsNotTakenForIt() = runBlocking {
        sentAfter = listOf(theSend.copy(timestamp = sendStartedAt / 1000 - 3_600), earlier)

        assertEquals(Delivery.NotFound("node-b"), failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt))
    }

    // A transfer in a block carries the block's time, which can be a little behind
    @Test
    fun transferTimedShortlyBeforeTheSendIsFound() = runBlocking {
        sentAfter = listOf(theSend.copy(timestamp = sendStartedAt / 1000 - 120), earlier)

        assertEquals(Delivery.Found("the-send"), failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt))
    }

    // Without the list from before, an older transfer cannot be told from the send
    @Test
    fun nothingIsFoundWhenTheListCannotBeReadBeforehand() = runBlocking {
        listFails = true

        assertEquals(Delivery.NotFound("node-b"), failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt))
        assertEquals(1, switches)
    }

    @Test
    fun deliveryIsLookedForOnTheSameNodeWhenThereIsNoOther() = runBlocking {
        otherNode = null
        sentAfter = listOf(theSend, earlier)

        assertEquals(Delivery.Found("the-send"), failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt))
    }

    @Test
    fun notFoundNamesNoNodeWhenThereIsNoOther() = runBlocking {
        otherNode = null

        assertEquals(Delivery.NotFound(null), failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt))
    }

    // The chain was already moved once on this screen, before the send
    @Test
    fun chainIsNotMovedASecondTimeToLookForTheSend() = runBlocking {
        failover.handle(BeforeBroadcast())

        assertEquals(Delivery.NotFound(null), failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt))
        assertEquals(1, switches)
    }

    @Test
    fun failedSwitchLeavesTheErrorAsItIs() = runBlocking {
        onSwitch = { throw IllegalStateException("adapter is null") }

        assertEquals(Outcome.Unhandled, failover.handle(BeforeBroadcast()))
    }

    @Test
    fun failedSwitchStillLooksForTheSend() = runBlocking {
        onSwitch = { throw IOException("no network") }

        assertEquals(Delivery.NotFound(null), failover.confirmDelivery(BigDecimal("1.5"), sendStartedAt))
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
