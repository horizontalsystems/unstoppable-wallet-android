package io.horizontalsystems.walletkit.chain.zcash

import io.horizontalsystems.walletkit.core.adapters.zcash.ZcashAdapter.ZcashSubmitException
import io.horizontalsystems.walletkit.core.chain.NodeError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZcashNodeErrorTest {

    // The adapter wraps what the submission raised
    private fun wrapped(cause: Throwable) = RuntimeException("Unexpected error while sending Zcash: ${cause.message}", cause)

    // The transaction is built and kept by the wallet, so it may have arrived or go out later
    @Test
    fun submissionTheServerNeverAnsweredMayHaveSent() {
        val error = wrapped(ZcashSubmitException("Transaction submission failed.", grpcError = true))

        assertEquals(NodeError(preBroadcast = false), zcashNodeError(error))
    }

    @Test
    fun transactionTheServerRefusedIsNotANodeFailure() {
        val error = wrapped(ZcashSubmitException("Transaction submission failed.", grpcError = false))

        assertNull(zcashNodeError(error))
    }

    @Test
    fun otherSendErrorsAreNotNodeFailures() {
        assertNull(zcashNodeError(wrapped(IllegalStateException("Transaction not attempted. TxId: abc"))))
        assertNull(zcashNodeError(IllegalArgumentException("Invalid proposal: bad memo")))
    }
}
