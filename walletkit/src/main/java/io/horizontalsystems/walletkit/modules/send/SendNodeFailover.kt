package io.horizontalsystems.walletkit.modules.send

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.chain.NodeError
import io.horizontalsystems.walletkit.core.chain.NodeSwitchUnfinishedException
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.monero.MoneroOutgoingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.zano.ZanoOutgoingTransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.FilterTransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import timber.log.Timber
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean

/**
 * What a confirmation screen does when the chain's node fails a fee estimate or a send, on a
 * chain that syncs from one node at a time (Monero, Zcash, Zano).
 *
 * A failure known to come before the broadcast sent nothing, so the chain is moved to another
 * node and the screen estimates again there; the user reviews and sends again. A failure that
 * may have delivered the transaction is never retried, because a second send could pay twice:
 * the screen says so and calls [confirmDelivery], which changes the node and looks for the
 * transfer in the wallet's list there.
 *
 * The node is changed at most once per screen. A second failure is shown as it is.
 */
class SendNodeFailover(
    private val token: Token,
    private val classify: (Throwable) -> NodeError?,
    private val switchNode: suspend (Token) -> String?,
    private val backgroundScope: CoroutineScope,
    // The newest transfers the wallet lists as sent, on the node the chain runs on now
    private val sentTransfers: suspend () -> List<SentTransfer> = { emptyList() },
    private val lookupTimeMs: Long = LOOKUP_TIME_MS,
    private val lookupIntervalMs: Long = LOOKUP_INTERVAL_MS,
    private val now: () -> Long = System::currentTimeMillis,
    private val wait: suspend (Long) -> Unit = { delay(it) },
    // The adapter the chain runs on now; a node change replaces it
    private val currentAdapter: () -> Any? = { null },
) {
    sealed class Outcome {
        /** Not a node failure, or no other node to move to: the error stands as it is. */
        object Unhandled : Outcome()

        /**
         * Nothing was sent and the chain now runs on [nodeName]. The adapter was rebuilt, so the
         * send service must be created again and the transfer estimated again.
         */
        data class Switched(val nodeName: String) : Outcome()

        /**
         * The transaction may have reached the network. It must not be sent again from here;
         * [confirmDelivery] finds out what can be found out.
         */
        object MaybeSent : Outcome()
    }

    /** Enough of a transfer the wallet lists as sent to recognise the one a failed send left. */
    data class SentTransfer(
        val hash: String,
        val amount: BigDecimal,
        val timestamp: Long,
        val toSelf: Boolean = false,
    )

    sealed class Delivery {
        /** The wallet lists the transfer: it was sent. */
        data class Found(val transactionHash: String) : Delivery()

        /**
         * The wallet does not list it. [nodeName] is the node the chain was moved to to look,
         * null if it was not moved. It may still have been sent.
         */
        data class NotFound(val nodeName: String?) : Delivery()
    }

    private val switched = AtomicBoolean(false)

    // A node change whose new adapter was not there in time: the node moved to, and the
    // adapter that was in use, which the screen's send service still holds
    private class UnfinishedSwitch(val nodeName: String, val staleAdapter: Any?)

    @Volatile
    private var unfinishedSwitch: UnfinishedSwitch? = null

    suspend fun handle(error: Throwable): Outcome {
        val nodeError = classify(error) ?: return Outcome.Unhandled

        if (!nodeError.preBroadcast) return Outcome.MaybeSent

        if (!switched.compareAndSet(false, true)) return Outcome.Unhandled
        return switchQuietly()?.let { Outcome.Switched(it) } ?: Outcome.Unhandled
    }

    /**
     * A node change that [handle] reported as [Outcome.Unhandled] because the chain's new
     * adapter was not there in time, once that adapter is there. Until the screen acts on it,
     * its send service holds the adapter that was stopped. Given once.
     */
    fun lateSwitch(): Outcome.Switched? {
        val unfinished = unfinishedSwitch ?: return null
        val adapter = currentAdapter()
        if (adapter == null || adapter === unfinished.staleAdapter) return null

        unfinishedSwitch = null
        return Outcome.Switched(unfinished.nodeName)
    }

    /**
     * After [Outcome.MaybeSent]: moves the chain to another node, where the wallet syncs and
     * shows what happened, and looks there for a transfer of [amount] that the wallet did not
     * list before. [sendStartedAt] is when the send began, in milliseconds.
     *
     * [toSelf] is a send to the wallet's own address. A wallet that did not record the send
     * itself may list such a transfer with another amount, such as that of its change, so it
     * is recognised by being a new transfer to self.
     *
     * The node is changed even if the caller stops waiting, as when the screen is closed.
     */
    suspend fun confirmDelivery(amount: BigDecimal, sendStartedAt: Long, toSelf: Boolean = false): Delivery {
        // Without the list from before, an older transfer cannot be told from this one
        val knownBefore = sentTransfersOrNull()?.map { it.hash }?.toSet()

        val nodeName = if (switched.compareAndSet(false, true)) {
            backgroundScope.async { switchQuietly() }.await()
        } else {
            null
        }
        if (knownBefore == null) return Delivery.NotFound(nodeName)

        val deadline = now() + lookupTimeMs
        while (true) {
            val found = sentTransfersOrNull()?.firstOrNull {
                it.hash !in knownBefore &&
                    (it.amount.compareTo(amount) == 0 || (toSelf && it.toSelf)) &&
                    it.timestamp * 1000 >= sendStartedAt - EARLIEST_MARGIN_MS
            }
            if (found != null) return Delivery.Found(found.hash)
            if (now() >= deadline) return Delivery.NotFound(nodeName)
            wait(lookupIntervalMs)
        }
    }

    private suspend fun sentTransfersOrNull(): List<SentTransfer>? = try {
        sentTransfers()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        null
    }

    private suspend fun switchQuietly(): String? = try {
        val adapterBefore = currentAdapter()
        try {
            switchNode(token)
        } catch (e: NodeSwitchUnfinishedException) {
            unfinishedSwitch = UnfinishedSwitch(e.nodeName, adapterBefore)
            null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Timber.e(e, "Node switch for a send failed on %s", token.blockchainType.uid)
        null
    }

    companion object {
        private val backgroundScope = CoroutineScope(Dispatchers.Default)

        // The node change ends with a synced wallet, so the transfer is listed soon after
        private const val LOOKUP_TIME_MS = 15_000L
        private const val LOOKUP_INTERVAL_MS = 1_000L

        // A transfer in a block carries the block's time, which can be minutes behind
        private const val EARLIEST_MARGIN_MS = 10 * 60 * 1000L

        // The transfer looked for is the newest one
        private const val SENT_LIST_SIZE = 20

        fun forToken(token: Token): SendNodeFailover {
            val plugin = ChainRegistry[token.blockchainType]
            return SendNodeFailover(
                token = token,
                classify = { error -> plugin?.classifyNodeError(error) },
                switchNode = { plugin?.failoverForSend(it) },
                backgroundScope = backgroundScope,
                sentTransfers = { sentTransfers(token) },
                currentAdapter = { App.adapterManager.getAdapterForToken<IAdapter>(token) },
            )
        }

        // Read anew on every call: the chain's adapter is rebuilt when its node changes
        private suspend fun sentTransfers(token: Token): List<SentTransfer> {
            val wallet = App.walletManager.activeWallets.firstOrNull { it.token == token } ?: return emptyList()
            val adapter = App.transactionAdapterManager.getAdapter(wallet.transactionSource) ?: return emptyList()
            return adapter
                .getTransactions(null, token, SENT_LIST_SIZE, FilterTransactionType.Outgoing, null)
                .mapNotNull { record ->
                    val amount = record.mainValue?.decimalValue?.abs() ?: return@mapNotNull null
                    SentTransfer(record.transactionHash, amount, record.timestamp, record.isToSelf())
                }
        }

        // The records of the chains that classify their send errors
        private fun TransactionRecord.isToSelf(): Boolean = when (this) {
            is ZanoOutgoingTransactionRecord -> sentToSelf
            is MoneroOutgoingTransactionRecord -> sentToSelf
            is BitcoinOutgoingTransactionRecord -> sentToSelf
            else -> false
        }
    }
}
