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
            // Block times looked up so far; a height neither source can date maps to null so it
            // is not looked up again.
            val resolvedBlockTimes = mutableMapOf<Long, Long?>()
            while (true) {
                val heightsToResolve = mutex.withLock {
                    // Decided against the current list, not the one seen on arrival: another
                    // batch may have changed a record this batch disagrees with while a lookup
                    // ran, and this batch's view of it is the newer one.
                    val newTransactions = transactionOverviews.filter { tx ->
                        transactions.none { known -> known.isSameAs(tx) }
                    }
                    val heights = heightsWithoutBlockTime(newTransactions, resolvedBlockTimes)
                    if (heights.isNotEmpty()) {
                        // Nothing to look up for a batch a newer one has already superseded.
                        if (batchOrder.isSuperseded(batch)) return@launch
                        return@withLock heights
                    }
                    // A newer batch stored while a lookup ran supersedes this one. A batch with
                    // nothing to store is accepted as well: the list already reflects it.
                    if (!batchOrder.accept(batch)) return@launch
                    if (newTransactions.isNotEmpty()) store(newTransactions, resolvedBlockTimes)
                    emptyList()
                }
                if (heightsToResolve.isEmpty()) return@launch

                // Off the lock: a server lookup may wait on the network, and the batch's other
                // transactions, as well as later batches, must not wait behind it.
                resolvedBlockTimes += resolveBlockTimes(heightsToResolve)
            }
        }
    }

    /** Mined heights of the transactions the SDK reports no block time for, not yet looked up. */
    private fun heightsWithoutBlockTime(overviews: List<TransactionOverview>, resolved: Map<Long, Long?>): List<Long> =
        overviews
            .filter { it.blockTimeEpochSeconds == null }
            .mapNotNull { it.minedHeight?.value }
            .filter { it !in resolved }
            .distinct()

    /**
     * Block times by height, looked up a few at a time so one slow lookup does not serialize
     * the rest; a height neither source can date maps to null.
     */
    private suspend fun resolveBlockTimes(heights: List<Long>): Map<Long, Long?> {
        val permits = Semaphore(MAX_PARALLEL_BLOCK_TIME_LOOKUPS)
        return coroutineScope {
            heights
                .map { height -> async { permits.withPermit { height to blockTime(height) } } }
                .awaitAll()
                .toMap()
        }
    }

    // Under the lock.
    private suspend fun store(newTransactions: List<TransactionOverview>, resolvedBlockTimes: Map<Long, Long?>) {
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

    /** Whether a batch received after [batch] has been stored. */
    fun isSuperseded(batch: Long): Boolean = batch < stored

    /** Whether [batch] may be stored: false once a batch received after it has been. */
    fun accept(batch: Long): Boolean {
        if (isSuperseded(batch)) return false
        stored = batch
        return true
    }
}
