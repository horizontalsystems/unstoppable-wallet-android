package io.horizontalsystems.walletkit.chain.monero

import io.horizontalsystems.walletkit.core.chain.NodeError

// How monero-kit words a daemon that failed a request: its own two messages for a transaction
// it could not build, and wallet2's for a node that stopped answering.
private val daemonFailures = listOf(
    "node is unreachable",
    "node is not responding",
    "failed to get",
    "failed to connect",
    "connection to daemon",
    "daemon is busy",
    "rpc error",
)

/**
 * Whether a failed Monero fee estimate or send was the daemon's failure. The kit builds the
 * transaction first and commits it second, and says which step failed: a daemon failure while
 * building sent nothing, one while committing may have delivered the transaction.
 */
internal fun moneroNodeError(error: Throwable): NodeError? {
    val message = error.message?.lowercase() ?: return null
    if (daemonFailures.none { it in message }) return null

    return NodeError(preBroadcast = !message.startsWith("send transaction failed"))
}
