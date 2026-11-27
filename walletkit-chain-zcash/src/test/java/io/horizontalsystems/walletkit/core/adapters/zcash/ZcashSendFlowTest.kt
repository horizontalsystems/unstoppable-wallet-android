package io.horizontalsystems.walletkit.core.adapters.zcash

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

// A Zcash send hands the transaction it built to other servers when its own gives no answer
class ZcashSendFlowTest {

    private val submitted = mutableListOf<Pair<String, String>>()
    private var builds = 0
    private var asksForOthers = 0

    private fun flow(
        transactions: List<String> = listOf("tx1"),
        others: List<String> = listOf("b", "c"),
        answer: (transaction: String, server: String) -> Submission,
    ) = ZcashSendFlow(
        create = { builds++; transactions },
        hashOf = { "hash-$it" },
        submit = { transaction, server -> submitted += transaction to server; answer(transaction, server) },
        ownServer = { "a" },
        otherServersThatAnswer = { asksForOthers++; others },
    )

    private fun run(flow: ZcashSendFlow<String, String>) = runBlocking { flow.run() }

    @Test
    fun sentThroughTheWalletsOwnServer() {
        val result = run(flow { _, _ -> Submission.Taken })

        assertEquals(ZcashSendResult.Sent(listOf("hash-tx1")), result)
        assertEquals(listOf("tx1" to "a"), submitted)
        assertEquals(0, asksForOthers)
    }

    @Test
    fun aRefusalByTheOwnServerIsNotTakenElsewhere() {
        val result = run(flow { _, _ -> Submission.Refused("bad transaction") })

        assertEquals(ZcashSendResult.NotSent("bad transaction"), result)
        assertEquals(listOf("tx1" to "a"), submitted)
    }

    @Test
    fun theSameTransactionGoesToAnotherServerWhenTheOwnOneGivesNoAnswer() {
        val result = run(flow { _, server -> if (server == "a") Submission.Unanswered("timeout") else Submission.Taken })

        assertEquals(ZcashSendResult.Sent(listOf("hash-tx1")), result)
        assertEquals(listOf("tx1" to "a", "tx1" to "b"), submitted)
        assertEquals(1, builds)
    }

    @Test
    fun unknownWhenNoServerAnswers() {
        val result = run(flow { _, server -> Submission.Unanswered("timeout on $server") })

        assertEquals(ZcashSendResult.Unknown("hash-tx1", "timeout on a"), result)
        assertEquals(listOf("tx1" to "a", "tx1" to "b", "tx1" to "c"), submitted)
    }

    @Test
    fun unknownWhenThereIsNoOtherServer() {
        val result = run(flow(others = emptyList()) { _, _ -> Submission.Unanswered("timeout") })

        assertEquals(ZcashSendResult.Unknown("hash-tx1", "timeout"), result)
        assertEquals(listOf("tx1" to "a"), submitted)
    }

    @Test
    fun noMoreThanTwoOtherServersAreTried() {
        run(flow(others = listOf("b", "c", "d", "e")) { _, _ -> Submission.Unanswered("timeout") })

        assertEquals(listOf("a", "b", "c"), submitted.map { it.second })
    }

    // The own server may have taken it before it went silent
    @Test
    fun aRefusalByAnotherServerLeavesItUnknown() {
        val result = run(
            flow { _, server -> if (server == "a") Submission.Unanswered("timeout") else Submission.Refused("conflict") }
        )

        assertEquals(ZcashSendResult.Unknown("hash-tx1", "timeout"), result)
        assertEquals(listOf("a", "b", "c"), submitted.map { it.second })
    }

    @Test
    fun aTransactionNoBlockCanTakeIsNotSentWhateverTheOwnServerDid() {
        val result = run(
            flow { _, server ->
                if (server == "a") Submission.Unanswered("timeout") else Submission.Refused("expired", neverMinable = true)
            }
        )

        assertEquals(ZcashSendResult.NotSent("expired"), result)
        assertEquals(listOf("a", "b"), submitted.map { it.second })
    }

    @Test
    fun everyTransactionOfATransferIsSubmittedInOrder() {
        val result = run(flow(transactions = listOf("tx1", "tx2")) { _, _ -> Submission.Taken })

        assertEquals(ZcashSendResult.Sent(listOf("hash-tx1", "hash-tx2")), result)
        assertEquals(listOf("tx1" to "a", "tx2" to "a"), submitted)
    }

    @Test
    fun aLaterTransactionIsNotSubmittedAfterOneThatIsUnknown() {
        val result = run(flow(transactions = listOf("tx1", "tx2"), others = emptyList()) { _, _ -> Submission.Unanswered("timeout") })

        assertEquals(ZcashSendResult.Unknown("hash-tx1", "timeout"), result)
        assertEquals(listOf("tx1"), submitted.map { it.first })
    }

    // The next transaction spends what the first one made, and only that server is known to have it
    @Test
    fun aTransfersNextTransactionGoesToTheServerThatTookTheFirst() {
        val result = run(
            flow(transactions = listOf("tx1", "tx2")) { _, server ->
                if (server == "a") Submission.Unanswered("timeout") else Submission.Taken
            }
        )

        assertEquals(ZcashSendResult.Sent(listOf("hash-tx1", "hash-tx2")), result)
        assertEquals(listOf("tx1" to "a", "tx1" to "b", "tx2" to "b"), submitted)
    }

    @Test
    fun theOtherServersAreFoundOnceForATransferOfSeveralTransactions() {
        // b takes the first transaction and gives no answer for the second, which goes on to c
        run(
            flow(transactions = listOf("tx1", "tx2")) { transaction, server ->
                val taken = server == "c" || (transaction == "tx1" && server == "b")
                if (taken) Submission.Taken else Submission.Unanswered("timeout")
            }
        )

        assertEquals(listOf("tx1" to "a", "tx1" to "b", "tx2" to "b", "tx2" to "c"), submitted)
        assertEquals(1, asksForOthers)
    }
}
