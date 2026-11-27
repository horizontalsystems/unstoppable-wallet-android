package io.horizontalsystems.walletkit.core.adapters.zcash

import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.TransactionSubmitResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// What the SDK's answer to one submission means for the send
class ZcashSubmissionTest {

    private val txId = FirstClassByteArray(ByteArray(32) { it.toByte() })

    private fun failure(grpcError: Boolean, code: Int = 3) =
        TransactionSubmitResult.Failure(txId, grpcError = grpcError, code = code, description = "why")

    @Test
    fun aSubmittedTransactionIsTaken() {
        assertEquals(Submission.Taken, TransactionSubmitResult.Success(txId).toSubmission())
    }

    @Test
    fun aCallThatFailedGaveNoAnswer() {
        assertTrue(failure(grpcError = true).toSubmission() is Submission.Unanswered)
    }

    @Test
    fun aServersRefusalSaysNothingOfOtherServers() {
        val refusal = failure(grpcError = false).toSubmission() as Submission.Refused

        assertEquals(false, refusal.neverMinable)
    }

    @Test
    fun aTransactionTheWalletHeldBackCanNeverBeMined() {
        val expired = failure(grpcError = false, code = TransactionSubmitResult.Failure.CODE_EXPIRED)
        val wrongBranch = failure(grpcError = false, code = TransactionSubmitResult.Failure.CODE_WRONG_CONSENSUS_BRANCH)

        assertEquals(true, (expired.toSubmission() as Submission.Refused).neverMinable)
        assertEquals(true, (wrongBranch.toSubmission() as Submission.Refused).neverMinable)
    }

    @Test
    fun aTransactionThatWasNotAttemptedIsRefused() {
        assertTrue(TransactionSubmitResult.NotAttempted(txId).toSubmission() is Submission.Refused)
    }
}
