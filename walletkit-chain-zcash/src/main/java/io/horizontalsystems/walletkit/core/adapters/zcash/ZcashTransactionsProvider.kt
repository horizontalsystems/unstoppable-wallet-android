package io.horizontalsystems.walletkit.core.adapters.zcash

import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.TransactionRecipient
import io.horizontalsystems.walletkit.modules.transactions.FilterTransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class ZcashTransactionsProvider(
    private val accountUuid: AccountUuid,
    private val scope: CoroutineScope,
    private val synchronizer: Synchronizer,
    private val isMigrationTransaction: (txHash: ByteArray) -> Boolean,
    // Block time of a mined height the SDK reports no block time for; null when unknown.
    private val blockTime: suspend (height: Long) -> Long? = { null },
) {
    private val mutex = Mutex()
    private var transactions = listOf<ZcashTransaction>()
    // Batches resolve block times off the lock and so may finish out of order; each carries the
    // whole transaction set, so a batch older than the one last stored must not overwrite it.
    private val batchOrder = TransactionBatchOrder()
    private val newTransactionsFlow = MutableSharedFlow<List<ZcashTransaction>>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun onTransactions(transactionOverviews: List<TransactionOverview>) {
        val batch = batchOrder.next()
        scope.launch {
            val newTransactions = mutex.withLock {
                val new = transactionOverviews.filter { tx ->
                    transactions.none { known -> known.isSameAs(tx) }
                }
                // Nothing to store: the list already reflects this batch.
                if (new.isEmpty()) batchOrder.accept(batch)
                new
            }
            if (newTransactions.isEmpty()) return@launch

            // Off the lock: a server lookup may wait on the network, and the batch's other
            // transactions, as well as later batches, must not wait behind it.
            val resolvedBlockTimes = resolveBlockTimes(newTransactions)

            mutex.withLock {
                // A newer batch stored while the lookup ran supersedes this one.
                if (!batchOrder.accept(batch)) return@withLock
                val newZcashTransactions = newTransactions.map {
                    val recipients = if (it.isSentTransaction) {
                        synchronizer.getRecipients(it)
                            .filterIsInstance<TransactionRecipient>()
                            .toList()
                    } else {
                        null
                    }
                    val memo = synchronizer.getMemos(it).firstOrNull()
                    val outputs = if (it.isSentTransaction) {
                        synchronizer.getTransactionOutputs(it)
                    } else {
                        emptyList()
                    }
                    val time = it.blockTimeEpochSeconds
                        ?: it.minedHeight?.value?.let { height -> resolvedBlockTimes[height] }
                    ZcashTransaction(accountUuid, it, recipients, memo, isMigrationTransaction(it.txId.value.byteArray), outputs, time)
                }
                newTransactionsFlow.tryEmit(newZcashTransactions)
                val notUpdatedTransactions =
                    transactions.filter { old -> newZcashTransactions.none { new -> new.transactionHash.contentEquals(old.transactionHash) } }
                transactions = (notUpdatedTransactions + newZcashTransactions).sortedDescending()
            }
        }
    }

    /**
     * Block times, by height, for the transactions the SDK reports no block time for. Each
     * distinct height is looked up once, a few at a time, so one slow lookup does not serialize
     * the rest; a height neither source can date is left out.
     */
    private suspend fun resolveBlockTimes(overviews: List<TransactionOverview>): Map<Long, Long> {
        val heights = overviews
            .filter { it.blockTimeEpochSeconds == null }
            .mapNotNull { it.minedHeight?.value }
            .distinct()
        if (heights.isEmpty()) return emptyMap()

        val permits = Semaphore(MAX_PARALLEL_BLOCK_TIME_LOOKUPS)
        return coroutineScope {
            heights
                .map { height -> async { permits.withPermit { height to blockTime(height) } } }
                .awaitAll()
                .mapNotNull { (height, time) -> time?.let { height to it } }
                .toMap()
        }
    }

    /**
     * Whether [tx] is the record this transaction was built from. A transaction found ahead of
     * the scan carries a resolved or estimated time; once its block is scanned the SDK reports
     * the block time, and that re-emission must replace the record rather than be skipped.
     */
    private fun ZcashTransaction.isSameAs(tx: TransactionOverview): Boolean =
        transactionHash.contentEquals(tx.txId.value.byteArray) &&
                minedHeight == tx.minedHeight?.value &&
                (tx.blockTimeEpochSeconds == null || tx.blockTimeEpochSeconds == timestamp)

    fun getNewTransactionsFlow(transactionType: FilterTransactionType, address: String?): Flow<List<ZcashTransaction>> {
        val filters = getFilters(transactionType, address)

        return if (filters.isEmpty()) {
            newTransactionsFlow
        } else {
            newTransactionsFlow.map { txs ->
                txs.filter { tx ->
                    filters.all { filter -> filter.invoke(tx) }
                }
            }.filter {
                it.isNotEmpty()
            }
        }
    }

    private fun getFilters(
        transactionType: FilterTransactionType,
        address: String?,
    ) = buildList<(ZcashTransaction) -> Boolean> {
        when (transactionType) {
            FilterTransactionType.All -> Unit
            FilterTransactionType.Incoming -> add { it.isIncoming }
            FilterTransactionType.Outgoing -> add { !it.isIncoming }
            FilterTransactionType.Swap,
            FilterTransactionType.Approve -> add { false }
        }

        if (address != null) {
            add { tx ->
                tx.recipients?.any { it.addressValue == address } ?: false
            }
        }
    }

    suspend fun getTransactions(
        from: Triple<ByteArray, Long, Int>?,
        transactionType: FilterTransactionType,
        address: String?,
        limit: Int,
    ): List<ZcashTransaction> {
        val filters = getFilters(transactionType, address)
        val filtered = when {
            filters.isEmpty() -> transactions
            else -> transactions.filter { tx -> filters.all { it.invoke(tx) } }
        }

        val fromIndex = from?.let { (transactionHash, timestamp, transactionIndex) ->
            filtered.indexOfFirst { it.transactionHash.contentEquals(transactionHash) && it.timestamp == timestamp && it.transactionIndex == transactionIndex } + 1
        } ?: 0

        return filtered.subList(fromIndex, min(filtered.size, fromIndex + limit))
    }

    companion object {
        private const val MAX_PARALLEL_BLOCK_TIME_LOOKUPS = 4
    }
}

/**
 * Orders the batches [ZcashTransactionsProvider.onTransactions] receives. Each batch is the
 * whole transaction set, so once one has been stored, every batch received before it is stale.
 * [accept] is called under the provider's lock.
 */
internal class TransactionBatchOrder {
    private val issued = AtomicLong()
    private var stored = 0L

    /** The sequence number of a batch just received. */
    fun next(): Long = issued.incrementAndGet()

    /** Whether [batch] may be stored: false once a batch received after it has been. */
    fun accept(batch: Long): Boolean {
        if (batch < stored) return false
        stored = batch
        return true
    }
}
