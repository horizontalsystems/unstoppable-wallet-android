package io.horizontalsystems.walletkit.core.adapters.zcash

import cash.z.ecc.android.sdk.model.TransactionSubmitResult

/** How a Zcash send ended. */
internal sealed class ZcashSendResult {
    /** Every transaction of the transfer is with a server. */
    data class Sent(val transactionHashes: List<String>) : ZcashSendResult()

    /** A clear answer that the transfer was not taken. Sending again cannot pay twice. */
    data class NotSent(val message: String) : ZcashSendResult()

    /**
     * No server the wallet may use answered. The wallet keeps the transaction and submits it
     * again by itself until it expires, so sending again can pay twice.
     */
    data class Unknown(val transactionHash: String, val message: String) : ZcashSendResult()
}

/** What handing one built transaction to one server came to. */
internal sealed class Submission {
    /** The server has the transaction. */
    object Taken : Submission()

    /**
     * A clear answer that the transaction was not taken.
     *
     * @param neverMinable the wallet itself held the transaction back because no block can
     * take it any more (it expired, or it was built for another consensus branch). True of
     * every server, whatever an earlier one did with it.
     */
    data class Refused(val message: String, val neverMinable: Boolean = false) : Submission()

    /** No answer: the server may have the transaction. */
    data class Unanswered(val message: String) : Submission()
}

/**
 * One Zcash send, start to end, with what the wallet does about a server that fails.
 *
 * A transfer is built from the wallet's own data and stored before anything is submitted, so
 * the only call that needs a server is the submission. When the wallet's own server gives no
 * answer, the same stored transaction is handed to other servers: a server that already has
 * it answers as taken, so this cannot pay twice. Nothing is built a second time.
 *
 * @param T a built transaction.
 * @param E a server.
 * @param create builds and stores the transfer's transactions, in the order they are to be
 * submitted. Submits nothing.
 * @param submit hands a built transaction to a server.
 * @param otherServersThatAnswer the other servers the wallet may use that answer, best first.
 * Asked only when the wallet's own server gave no answer.
 */
internal class ZcashSendFlow<T, E>(
    private val create: suspend () -> List<T>,
    private val hashOf: (T) -> String,
    private val submit: suspend (T, E) -> Submission,
    private val ownServer: () -> E,
    private val otherServersThatAnswer: suspend () -> List<E>,
) {
    // Found once: a transfer of several transactions goes on through the same servers
    private var others: List<E>? = null

    // The server that took the last transaction. A transfer's next transaction spends what
    // that one made, so it goes to the server that is known to have it
    private var taker: E? = null

    suspend fun run(): ZcashSendResult {
        val sent = mutableListOf<String>()
        for (transaction in create()) {
            val failure = when (val outcome = submitThroughServers(transaction)) {
                is Submission.Taken -> null
                is Submission.Refused -> ZcashSendResult.NotSent(outcome.message)
                is Submission.Unanswered -> ZcashSendResult.Unknown(hashOf(transaction), outcome.message)
            }
            if (failure != null) return failure
            sent += hashOf(transaction)
        }
        return ZcashSendResult.Sent(sent)
    }

    private suspend fun submitThroughServers(transaction: T): Submission {
        val start = taker ?: ownServer()
        val first = submit(transaction, start)
        return if (first is Submission.Unanswered) settledElsewhere(transaction, start) ?: first else first
    }

    // The answer that settles what became of the transaction, from the first other server to
    // give one
    private suspend fun settledElsewhere(transaction: T, tried: E): Submission? {
        for (server in otherServers().filter { it != tried }) {
            val next = submit(transaction, server)
            if (next is Submission.Taken) taker = server
            // A refusal by a server's mempool is not proof that nothing is out when an
            // earlier server gave no answer; one the wallet made itself is true of them all
            val settled = next is Submission.Taken || (next is Submission.Refused && next.neverMinable)
            if (settled) return next
        }
        return null
    }

    private suspend fun otherServers(): List<E> =
        others ?: otherServersThatAnswer().take(MAX_OTHER_SERVERS).also { others = it }

    companion object {
        const val MAX_OTHER_SERVERS = 2
    }
}

/** What the SDK's answer to one submission means for the send. */
internal fun TransactionSubmitResult.toSubmission(): Submission = when (this) {
    is TransactionSubmitResult.Success -> Submission.Taken

    is TransactionSubmitResult.Failure -> {
        val message = "Transaction submission failed. TxId: ${txIdString()}, gRPC error: $grpcError, " +
            "Code: $code, Description: ${description ?: "None"}"
        if (grpcError) {
            Submission.Unanswered(message)
        } else {
            Submission.Refused(message, neverMinable = isExpired || isWrongConsensusBranch)
        }
    }

    is TransactionSubmitResult.NotAttempted -> Submission.Refused("Transaction not attempted. TxId: ${txIdString()}")
}
