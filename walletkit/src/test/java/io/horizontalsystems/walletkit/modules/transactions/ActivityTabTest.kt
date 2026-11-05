package io.horizontalsystems.walletkit.modules.transactions

import io.horizontalsystems.walletkit.modules.multiswap.history.SwapOperation
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapStatus
import io.horizontalsystems.walletkit.uiv3.components.TransactionIconState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class ActivityTabTest {

    private val confidential = setOf("u_NEAR_CONFIDENTIAL")

    @Test
    fun `transfers shows every kind of swap record`() {
        SwapRecordKind.entries.forEach {
            assertTrue(it.name, ActivityTab.Transfers.shows(it))
        }
    }

    @Test
    fun `swaps shows public and private swaps only`() {
        assertTrue(ActivityTab.Swaps.shows(SwapRecordKind.Swap))
        assertTrue(ActivityTab.Swaps.shows(SwapRecordKind.PrivateSwap))
        assertFalse(ActivityTab.Swaps.shows(SwapRecordKind.PrivateSend))
        assertFalse(ActivityTab.Swaps.shows(SwapRecordKind.CrossPay))
    }

    @Test
    fun `privates shows private swaps and private sends only`() {
        assertFalse(ActivityTab.Privates.shows(SwapRecordKind.Swap))
        assertTrue(ActivityTab.Privates.shows(SwapRecordKind.PrivateSwap))
        assertTrue(ActivityTab.Privates.shows(SwapRecordKind.PrivateSend))
        assertFalse(ActivityTab.Privates.shows(SwapRecordKind.CrossPay))
    }

    @Test
    fun `cross pays shows cross pays only`() {
        assertFalse(ActivityTab.CrossPays.shows(SwapRecordKind.Swap))
        assertFalse(ActivityTab.CrossPays.shows(SwapRecordKind.PrivateSwap))
        assertFalse(ActivityTab.CrossPays.shows(SwapRecordKind.PrivateSend))
        assertTrue(ActivityTab.CrossPays.shows(SwapRecordKind.CrossPay))
    }

    @Test
    fun `a swap is private when split or routed through a confidential provider`() {
        assertEquals(SwapRecordKind.Swap, SwapRecordKind.of(SwapOperation.Swap, "u_THORCHAIN", confidential))
        assertEquals(SwapRecordKind.PrivateSwap, SwapRecordKind.of(SwapOperation.Swap, "u_NEAR_CONFIDENTIAL", confidential))
        assertEquals(SwapRecordKind.PrivateSwap, SwapRecordKind.of(SwapOperation.SplitSwap, "u_SWAPUZ", confidential))
    }

    @Test
    fun `private sends and cross pays keep their kind whatever the provider`() {
        assertEquals(SwapRecordKind.PrivateSend, SwapRecordKind.of(SwapOperation.PrivateSend, "u_NEAR_CONFIDENTIAL", confidential))
        assertEquals(SwapRecordKind.CrossPay, SwapRecordKind.of(SwapOperation.CrossPay, "u_NEAR_CONFIDENTIAL", confidential))
        assertEquals(SwapRecordKind.CrossPay, SwapRecordKind.of(SwapOperation.CrossPay, "u_THORCHAIN", confidential))
    }

    @Test
    fun `a record in progress shows the pending state`() {
        assertEquals(TransactionIconState.Pending, swapRecordIconState(SwapStatus.Depositing))
        assertEquals(TransactionIconState.Pending, swapRecordIconState(SwapStatus.Swapping))
        assertEquals(TransactionIconState.Pending, swapRecordIconState(SwapStatus.Sending))
    }

    @Test
    fun `a completed record shows the completed state`() {
        assertEquals(TransactionIconState.Completed, swapRecordIconState(SwapStatus.Completed))
    }

    @Test
    fun `failed, refunded, expired and action required records show the failed state`() {
        assertEquals(TransactionIconState.Failed, swapRecordIconState(SwapStatus.Failed))
        assertEquals(TransactionIconState.Failed, swapRecordIconState(SwapStatus.Refunded))
        assertEquals(TransactionIconState.Failed, swapRecordIconState(SwapStatus.Expired))
        assertEquals(TransactionIconState.Failed, swapRecordIconState(SwapStatus.ActionRequired))
    }

    @Test
    fun `transfers merges transactions and records newest first`() {
        val merged = mergeActivity(
            transactions = listOf(Row("tx3", 3) to "0x3", Row("tx1", 1) to "0x1"),
            swapRecords = listOf(Row("sr4", 4), Row("sr2", 2)),
            depositHashes = emptySet(),
            date = { it.date },
        )

        assertEquals(listOf("sr4", "tx3", "sr2", "tx1"), merged.map { it.id })
    }

    @Test
    fun `a swap deposit is listed once, as its record`() {
        val merged = mergeActivity(
            transactions = listOf(Row("deposit", 2) to "0xAbC", Row("other", 1) to "0xdef"),
            swapRecords = listOf(Row("record", 3)),
            depositHashes = setOf("0xabc"),
            date = { it.date },
        )

        assertEquals(listOf("record", "other"), merged.map { it.id })
    }

    private data class Row(val id: String, val seconds: Long) {
        val date = Date(seconds * 1000)
    }
}
