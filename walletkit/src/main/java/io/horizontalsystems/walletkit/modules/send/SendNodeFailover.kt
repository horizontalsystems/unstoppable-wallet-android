package io.horizontalsystems.walletkit.modules.send

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.chain.NodeError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * What a confirmation screen does when the chain's node fails a fee estimate or a send, on a
 * chain that syncs from one node at a time (Monero, Zcash, Zano).
 *
 * A failure known to come before the broadcast sent nothing, so the chain is moved to another
 * node and the screen estimates again there; the user reviews and sends again. A failure that
 * may have delivered the transaction is never retried, because a second send could pay twice:
 * the node is still changed, in the background, so that the wallet syncs and shows what
 * happened.
 *
 * The node is changed at most once per screen. A second failure is shown as it is.
 */
class SendNodeFailover(
    private val token: Token,
    private val classify: (Throwable) -> NodeError?,
    private val switchNode: suspend (Token) -> String?,
    private val backgroundScope: CoroutineScope,
) {
    sealed class Outcome {
        /** Not a node failure, or no other node to move to: the error stands as it is. */
        object Unhandled : Outcome()

        /**
         * Nothing was sent and the chain now runs on [nodeName]. The adapter was rebuilt, so the
         * send service must be created again and the transfer estimated again.
         */
        data class Switched(val nodeName: String) : Outcome()

        /** The transaction may have reached the network. It must not be sent again from here. */
        object MaybeSent : Outcome()
    }

    private val switched = AtomicBoolean(false)

    suspend fun handle(error: Throwable): Outcome {
        val nodeError = classify(error) ?: return Outcome.Unhandled

        if (!nodeError.preBroadcast) {
            // Not waited for: the screen has nothing to retry, it only needs a wallet that syncs.
            if (switched.compareAndSet(false, true)) {
                backgroundScope.launch { switchQuietly() }
            }
            return Outcome.MaybeSent
        }

        if (!switched.compareAndSet(false, true)) return Outcome.Unhandled
        return switchQuietly()?.let { Outcome.Switched(it) } ?: Outcome.Unhandled
    }

    private suspend fun switchQuietly(): String? = try {
        switchNode(token)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Timber.e(e, "Node switch for a send failed on %s", token.blockchainType.uid)
        null
    }

    companion object {
        private val backgroundScope = CoroutineScope(Dispatchers.Default)

        /**
         * @param preBroadcastOnly leave a failure that may have delivered the transaction to
         * the screen as it is, node included. For a screen that stays usable after it and so
         * must not have its adapter rebuilt in the background.
         */
        fun forToken(token: Token, preBroadcastOnly: Boolean = false): SendNodeFailover {
            val plugin = ChainRegistry[token.blockchainType]
            return SendNodeFailover(
                token = token,
                classify = { error ->
                    plugin?.classifyNodeError(error)?.takeIf { it.preBroadcast || !preBroadcastOnly }
                },
                switchNode = { plugin?.failoverForSend(it) },
                backgroundScope = backgroundScope,
            )
        }
    }
}
