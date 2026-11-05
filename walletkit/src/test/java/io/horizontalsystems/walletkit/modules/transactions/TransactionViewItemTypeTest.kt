package io.horizontalsystems.walletkit.modules.transactions

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.transactions.TransactionViewItem.Type
import io.horizontalsystems.walletkit.uiv3.components.TransactionIconState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionViewItemTypeTest {

    @Test
    fun `pending and processing transactions show the pending state`() {
        assertEquals(TransactionIconState.Pending, transactionIconState(TransactionStatus.Pending))
        assertEquals(TransactionIconState.Pending, transactionIconState(TransactionStatus.Processing(0.5f)))
    }

    @Test
    fun `completed and failed transactions keep their state`() {
        assertEquals(TransactionIconState.Completed, transactionIconState(TransactionStatus.Completed))
        assertEquals(TransactionIconState.Failed, transactionIconState(TransactionStatus.Failed))
    }

    @Test
    fun `a completed row has no state text`() {
        Type.entries.forEach {
            assertNull(it.name, it.stateText(TransactionIconState.Completed))
        }
    }

    @Test
    fun `a pending row names what is in progress`() {
        assertEquals(R.string.Transactions_Status_Receiving, Type.Receive.stateText(TransactionIconState.Pending))
        assertEquals(R.string.Transactions_Status_Sending, Type.Send.stateText(TransactionIconState.Pending))
        assertEquals(R.string.Transactions_Status_Sending, Type.SendToSelf.stateText(TransactionIconState.Pending))
        assertEquals(R.string.Transactions_Status_Approving, Type.Approve.stateText(TransactionIconState.Pending))
        assertEquals(R.string.Transactions_Status_Swapping, Type.Swap.stateText(TransactionIconState.Pending))
        assertEquals(R.string.Transactions_Status_Shielding, Type.Shield.stateText(TransactionIconState.Pending))
        assertEquals(R.string.Transactions_Status_Unshielding, Type.Unshield.stateText(TransactionIconState.Pending))
        assertEquals(R.string.Transactions_Status_Pending, Type.Contract.stateText(TransactionIconState.Pending))
        assertEquals(R.string.Transactions_Status_Pending, Type.Unknown.stateText(TransactionIconState.Pending))
    }

    @Test
    fun `a failed row names what failed`() {
        assertEquals(R.string.Transactions_Status_TransferFailed, Type.Receive.stateText(TransactionIconState.Failed))
        assertEquals(R.string.Transactions_Status_TransferFailed, Type.Send.stateText(TransactionIconState.Failed))
        assertEquals(R.string.Transactions_Status_TransferFailed, Type.SendToSelf.stateText(TransactionIconState.Failed))
        assertEquals(R.string.Transactions_Status_ApprovalFailed, Type.Approve.stateText(TransactionIconState.Failed))
        assertEquals(R.string.Transactions_Status_SwapFailed, Type.Swap.stateText(TransactionIconState.Failed))
        assertEquals(R.string.Transactions_Status_ShieldingFailed, Type.Shield.stateText(TransactionIconState.Failed))
        assertEquals(R.string.Transactions_Status_UnshieldFailed, Type.Unshield.stateText(TransactionIconState.Failed))
        assertEquals(R.string.Transactions_Status_TransactionFailed, Type.Contract.stateText(TransactionIconState.Failed))
        assertEquals(R.string.Transactions_Status_TransactionFailed, Type.Unknown.stateText(TransactionIconState.Failed))
    }

    @Test
    fun `each type shows its design icon`() {
        assertEquals(R.drawable.arrow_m_down_24, Type.Receive.icon)
        assertEquals(R.drawable.arrow_m_up_24, Type.Send.icon)
        assertEquals(R.drawable.arrow_return_24, Type.SendToSelf.icon)
        assertEquals(R.drawable.unlock_24, Type.Approve.icon)
        assertEquals(R.drawable.swap_e_24, Type.Swap.icon)
        assertEquals(R.drawable.shield_24, Type.Shield.icon)
        assertEquals(R.drawable.shield_off_24, Type.Unshield.icon)
        assertEquals(R.drawable.list_24, Type.Contract.icon)
        assertEquals(R.drawable.warning_24, Type.Unknown.icon)
    }
}
