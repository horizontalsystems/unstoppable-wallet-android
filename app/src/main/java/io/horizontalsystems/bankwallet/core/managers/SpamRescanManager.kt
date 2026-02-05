package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.EvmTransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.StellarTransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.TronTransactionsAdapter
import io.horizontalsystems.bankwallet.core.storage.ScannedTransactionStorage
import io.horizontalsystems.bankwallet.entities.ScannedTransaction
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.stellarkit.room.Operation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import io.horizontalsystems.tronkit.models.FullTransaction as TronFullTransaction

class SpamRescanManager(
    private val scannedTransactionStorage: ScannedTransactionStorage,
    private val spamManager: SpamManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val scanMutex = Mutex()

    // Track running scans per source (sourceKey -> scan job info)
    private val runningScans = mutableMapOf<String, ScanJob>()

    // Pending waiters per source (sourceKey -> (txHashHex -> list of deferreds))
    private val pendingWaitersPerSource = mutableMapOf<String, MutableMap<String, MutableList<CompletableDeferred<Unit>>>>()
    private val waitersMutex = Mutex()

    companion object {
        private const val TAG = "SpamRescanManager"
        private const val BATCH_SIZE = 50
        private const val OUTGOING_CONTEXT_SIZE = 10
        private const val SCAN_COMPLETE_MARKER = "complete"
        private const val WAITER_TIMEOUT_MS = 60_000L // 1 minute timeout for waiters
    }

    private data class ScanJob(
        val sourceKey: String,
        var isRunning: Boolean = true
    )

    private fun getSourceKey(source: TransactionSource): String {
        return "${source.blockchain.type.uid}:${source.account.id}"
    }

    /**
     * Ensure a transaction is scanned. If not yet scanned, triggers scan and waits.
     * Returns the ScannedTransaction if found after scan, null otherwise.
     * Note: Caller should check DB first before calling this to avoid redundant lookups.
     */
    suspend fun ensureTransactionScanned(
        transactionHash: ByteArray,
        source: TransactionSource,
        adapter: ITransactionsAdapter
    ): ScannedTransaction? {
        val txHashHex = transactionHash.toHexString()
        val sourceKey = getSourceKey(source)

        // Check if scan is complete for this source (no point waiting)
        val scanState = scannedTransactionStorage.getSpamScanState(
            source.blockchain.type,
            source.account.id
        )
        if (scanState?.lastSyncedTransactionId == SCAN_COMPLETE_MARKER) {
            return null
        }

        // Need to scan - create a deferred to wait for this transaction
        val deferred = CompletableDeferred<Unit>()

        waitersMutex.withLock {
            pendingWaitersPerSource
                .getOrPut(sourceKey) { mutableMapOf() }
                .getOrPut(txHashHex) { mutableListOf() }
                .add(deferred)
        }

        // Start scan if not already running
        startScanIfNeeded(source, adapter)

        // Wait for transaction to be scanned with timeout
        val result = withTimeoutOrNull(WAITER_TIMEOUT_MS) {
            try {
                deferred.await()
                true
            } catch (_: Throwable) {
                false
            }
        }

        // Clean up if timed out or failed
        if (result != true) {
            waitersMutex.withLock {
                pendingWaitersPerSource[sourceKey]?.get(txHashHex)?.remove(deferred)
            }
        }

        // Return the scanned transaction (or null if not found)
        return scannedTransactionStorage.getScannedTransaction(transactionHash)
    }

    private fun startScanIfNeeded(source: TransactionSource, adapter: ITransactionsAdapter) {
        val sourceKey = getSourceKey(source)

        coroutineScope.launch {
            // Check and mark as running atomically
            val shouldRun = scanMutex.withLock {
                if (runningScans[sourceKey]?.isRunning == true) {
                    false
                } else {
                    runningScans[sourceKey] = ScanJob(sourceKey)
                    true
                }
            }

            if (!shouldRun) return@launch

            // Run the actual scan
            try {
                rescanTransactionsForSource(source, adapter)
            } catch (e: Throwable) {
                Log.e(TAG, "Error scanning transactions for $sourceKey", e)
                notifyWaitersForSource(sourceKey)
            } finally {
                scanMutex.withLock {
                    runningScans.remove(sourceKey)
                }
            }
        }
    }

    private suspend fun rescanTransactionsForSource(
        source: TransactionSource,
        adapter: ITransactionsAdapter
    ) {
        val sourceKey = getSourceKey(source)

        when (adapter) {
            is EvmTransactionsAdapter -> {
                val userAddress = adapter.evmKitWrapper.evmKit.receiveAddress
                val baseToken = adapter.baseToken

                rescanWithStrategy<FullTransaction, ByteArray>(
                    source = source,
                    sourceKey = sourceKey,
                    fetchBatch = { lastHash -> adapter.getFullTransactionsBefore(lastHash, BATCH_SIZE) },
                    getTimestamp = { tx -> tx.transaction.timestamp },
                    getTxHashHex = { tx -> tx.transaction.hash.toHexString() },
                    getNextCursor = { transactions -> transactions.lastOrNull()?.transaction?.hash },
                    getCursorString = { hash -> hash?.toHexString() },
                    processTransaction = { tx, context ->
                        spamManager.processEvmTransactionForSpamWithContext(tx, source, userAddress, baseToken, context)
                    },
                    extractOutgoingInfo = { tx -> spamManager.evmExtractor.extractOutgoingInfo(tx, userAddress) }
                )
            }

            is TronTransactionsAdapter -> {
                val userAddress = adapter.tronKitWrapper.tronKit.address
                val baseToken = spamManager.getBaseToken(source)
                if (baseToken == null) {
                    Log.w(TAG, "Could not get base token for Tron source: $sourceKey")
                    notifyWaitersForSource(sourceKey)
                    return
                }

                rescanWithStrategy<TronFullTransaction, ByteArray>(
                    source = source,
                    sourceKey = sourceKey,
                    fetchBatch = { lastHash -> adapter.getTronFullTransactionsBefore(lastHash, BATCH_SIZE) },
                    getTimestamp = { tx -> tx.transaction.timestamp },
                    getTxHashHex = { tx -> tx.transaction.hash.toHexString() },
                    getNextCursor = { transactions -> transactions.lastOrNull()?.transaction?.hash },
                    getCursorString = { hash -> hash?.toHexString() },
                    processTransaction = { tx, context ->
                        spamManager.processTronTransactionForSpamWithContext(tx, source, userAddress, baseToken, context)
                    },
                    extractOutgoingInfo = { tx -> spamManager.tronExtractor.extractOutgoingInfo(tx, userAddress) }
                )
            }

            is StellarTransactionsAdapter -> {
                val selfAddress = adapter.stellarKitWrapper.stellarKit.receiveAddress
                val baseToken = spamManager.getBaseToken(source)
                if (baseToken == null) {
                    Log.w(TAG, "Could not get base token for Stellar source: $sourceKey")
                    notifyWaitersForSource(sourceKey)
                    return
                }

                rescanWithStrategy<Operation, Long>(
                    source = source,
                    sourceKey = sourceKey,
                    fetchBatch = { lastId -> adapter.getStellarOperationsBefore(lastId, BATCH_SIZE) },
                    getTimestamp = { op -> op.timestamp },
                    getTxHashHex = { op -> op.transactionHash },
                    getNextCursor = { operations -> operations.lastOrNull()?.id },
                    getCursorString = { id -> id?.toString() },
                    processTransaction = { op, context ->
                        spamManager.processStellarOperationForSpamWithContext(op, source, selfAddress, baseToken, context)
                    },
                    extractOutgoingInfo = { op -> spamManager.stellarExtractor.extractOutgoingInfo(op, selfAddress) }
                )
            }

            else -> {
                Log.w(TAG, "Unsupported adapter type for source: $sourceKey")
                notifyWaitersForSource(sourceKey)
            }
        }
    }

    /**
     * Generic rescan strategy that works with any transaction type.
     */
    private suspend fun <T, C> rescanWithStrategy(
        source: TransactionSource,
        sourceKey: String,
        fetchBatch: suspend (cursor: C?) -> List<T>,
        getTimestamp: (T) -> Long,
        getTxHashHex: (T) -> String,
        getNextCursor: (List<T>) -> C?,
        getCursorString: (C?) -> String?,
        processTransaction: (T, List<PoisoningScorer.OutgoingTxInfo>) -> Unit,
        extractOutgoingInfo: (T) -> PoisoningScorer.OutgoingTxInfo?
    ) {
        try {
            var cursor: C? = null
            val outgoingContext = mutableListOf<PoisoningScorer.OutgoingTxInfo>()

            while (true) {
                val items = fetchBatch(cursor)
                if (items.isEmpty()) break

                // Sort by timestamp (oldest first) for correct outgoing context
                val sortedItems = items.sortedBy { getTimestamp(it) }

                for (item in sortedItems) {
                    val txHashHex = getTxHashHex(item)

                    // Process the transaction
                    processTransaction(item, outgoingContext.toList())

                    // Notify any waiters for this transaction
                    notifyWaiter(sourceKey, txHashHex)

                    // Update outgoing context
                    extractOutgoingInfo(item)?.let { outgoingInfo ->
                        outgoingContext.add(0, outgoingInfo)
                        if (outgoingContext.size > OUTGOING_CONTEXT_SIZE) {
                            outgoingContext.removeAt(outgoingContext.size - 1)
                        }
                    }
                }

                cursor = getNextCursor(items)

                // Update progress
                getCursorString(cursor)?.let { cursorStr ->
                    scannedTransactionStorage.save(
                        SpamScanState(source.blockchain.type, source.account.id, cursorStr)
                    )
                }

                if (items.size < BATCH_SIZE) break
            }

            // Mark as complete
            scannedTransactionStorage.save(
                SpamScanState(source.blockchain.type, source.account.id, SCAN_COMPLETE_MARKER)
            )

            // Notify remaining waiters for this source
            notifyWaitersForSource(sourceKey)

        } catch (e: Throwable) {
            Log.e(TAG, "Error during rescan for $sourceKey", e)
            notifyWaitersForSource(sourceKey)
        }
    }

    private suspend fun notifyWaiter(sourceKey: String, txHashHex: String) {
        waitersMutex.withLock {
            pendingWaitersPerSource[sourceKey]?.remove(txHashHex)?.forEach { deferred ->
                deferred.complete(Unit)
            }
        }
    }

    private suspend fun notifyWaitersForSource(sourceKey: String) {
        waitersMutex.withLock {
            pendingWaitersPerSource.remove(sourceKey)?.values?.flatten()?.forEach { deferred ->
                deferred.complete(Unit)
            }
        }
    }
}