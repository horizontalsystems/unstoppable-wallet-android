package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.EvmTransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.TronTransactionsAdapter
import io.horizontalsystems.bankwallet.core.storage.ScannedTransactionStorage
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.toHexString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SpamRescanManager(
    private val scannedTransactionStorage: ScannedTransactionStorage,
    private val spamManager: SpamManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val scanMutex = Mutex()

    // Track running scans per source (sourceKey -> scan job info)
    private val runningScans = mutableMapOf<String, ScanJob>()

    // Pending waiters for specific transactions (txHashHex -> list of deferreds)
    private val pendingWaiters = mutableMapOf<String, MutableList<CompletableDeferred<Unit>>>()
    private val waitersMutex = Mutex()

    companion object {
        private const val BATCH_SIZE = 50
        private const val OUTGOING_CONTEXT_SIZE = 10
        private const val SCAN_COMPLETE_MARKER = "complete"
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
     * Returns true if transaction was found in DB after scan, false otherwise.
     */
    suspend fun ensureTransactionScanned(
        transactionHash: ByteArray,
        source: TransactionSource,
        adapter: ITransactionsAdapter
    ): Boolean {
        val txHashHex = transactionHash.toHexString()

        // Check if already scanned
        scannedTransactionStorage.getScannedTransaction(transactionHash)?.let {
            return true
        }

        // Check if scan is complete for this source
        val scanState = scannedTransactionStorage.getSpamScanState(
            source.blockchain.type,
            source.account.id
        )
        if (scanState?.lastSyncedTransactionId == SCAN_COMPLETE_MARKER) {
            // Scan complete but transaction not found - it wasn't in DB when scan ran
            // This shouldn't happen in normal flow, but return false
            return false
        }

        // Need to scan - create a deferred to wait for this transaction
        val deferred = CompletableDeferred<Unit>()

        waitersMutex.withLock {
            pendingWaiters.getOrPut(txHashHex) { mutableListOf() }.add(deferred)
        }

        // Start scan if not already running
        startScanIfNeeded(source, adapter)

        // Wait for transaction to be scanned (with timeout protection)
        try {
            deferred.await()
        } catch (_: Throwable) {
            // Timeout or cancellation - clean up
            waitersMutex.withLock {
                pendingWaiters[txHashHex]?.remove(deferred)
            }
        }

        // Check if now in database
        return scannedTransactionStorage.getScannedTransaction(transactionHash) != null
    }

    private fun startScanIfNeeded(source: TransactionSource, adapter: ITransactionsAdapter) {
        val sourceKey = getSourceKey(source)

        coroutineScope.launch {
            scanMutex.withLock {
                // Check if already running
                if (runningScans[sourceKey]?.isRunning == true) {
                    return@launch
                }

                // Mark as running
                runningScans[sourceKey] = ScanJob(sourceKey)
            }

            // Run the actual scan
            try {
                rescanTransactionsForSource(source, adapter)
            } finally {
                scanMutex.withLock {
                    runningScans[sourceKey]?.isRunning = false
                }
            }
        }
    }

    private suspend fun rescanTransactionsForSource(
        source: TransactionSource,
        adapter: ITransactionsAdapter
    ) {
        when (adapter) {
            is EvmTransactionsAdapter -> rescanEvmTransactions(source, adapter)
            is TronTransactionsAdapter -> rescanTronTransactions(source, adapter)
            is io.horizontalsystems.bankwallet.core.adapters.StellarTransactionsAdapter -> rescanStellarTransactions(source, adapter)
            else -> {
                // Unsupported adapter - notify waiters and return
                notifyAllRemainingWaiters()
            }
        }
    }

    private suspend fun rescanEvmTransactions(
        source: TransactionSource,
        adapter: EvmTransactionsAdapter
    ) {
        val userAddress = adapter.evmKitWrapper.evmKit.receiveAddress
        val baseToken = adapter.baseToken

        try {
            var lastTxHash: ByteArray? = null
            val outgoingContext = mutableListOf<PoisoningScorer.OutgoingTxInfo>()

            while (true) {
                val transactions = adapter.getFullTransactionsBefore(lastTxHash, BATCH_SIZE)
                if (transactions.isEmpty()) break

                // Sort by timestamp (oldest first) for correct outgoing context
                val sortedTransactions = transactions.sortedBy { it.transaction.timestamp }

                sortedTransactions.forEach { fullTx ->
                    val txHashHex = fullTx.transaction.hash.toHexString()

                    // Process the transaction
                    spamManager.processEvmTransactionForSpamWithContext(
                        fullTx,
                        source,
                        userAddress,
                        baseToken,
                        outgoingContext.toList()
                    )

                    // Notify any waiters for this transaction
                    notifyWaiters(txHashHex)

                    // Update outgoing context
                    spamManager.extractOutgoingInfo(fullTx, userAddress)?.let { outgoingInfo ->
                        outgoingContext.add(0, outgoingInfo)
                        if (outgoingContext.size > OUTGOING_CONTEXT_SIZE) {
                            outgoingContext.removeAt(outgoingContext.size - 1)
                        }
                    }
                }

                lastTxHash = transactions.lastOrNull()?.transaction?.hash

                // Update progress
                lastTxHash?.toHexString()?.let {
                    scannedTransactionStorage.save(
                        SpamScanState(source.blockchain.type, source.account.id, it)
                    )
                }

                if (transactions.size < BATCH_SIZE) break
            }

            // Mark as complete
            scannedTransactionStorage.save(
                SpamScanState(source.blockchain.type, source.account.id, SCAN_COMPLETE_MARKER)
            )

            // Notify all remaining waiters (transaction might not exist in blockchain data)
            notifyAllRemainingWaiters()

        } catch (_: Throwable) {
            // Error during scan - notify waiters so they don't hang forever
            notifyAllRemainingWaiters()
        }
    }

    private suspend fun rescanTronTransactions(
        source: TransactionSource,
        adapter: TronTransactionsAdapter
    ) {
        val userAddress = adapter.tronKitWrapper.tronKit.address
        val baseToken = spamManager.getBaseToken(source) ?: return

        try {
            var lastTxHash: ByteArray? = null
            val outgoingContext = mutableListOf<PoisoningScorer.OutgoingTxInfo>()

            while (true) {
                val transactions: List<io.horizontalsystems.tronkit.models.FullTransaction> = adapter.getTronFullTransactionsBefore(lastTxHash, BATCH_SIZE)
                if (transactions.isEmpty()) break

                // Sort by timestamp (oldest first) for correct outgoing context
                val sortedTransactions = transactions.sortedBy { it.transaction.timestamp }

                for (fullTx in sortedTransactions) {
                    val txHashHex = fullTx.transaction.hash.toHexString()

                    // Process the transaction
                    spamManager.processTronTransactionForSpamWithContext(
                        fullTx,
                        source,
                        userAddress,
                        baseToken,
                        outgoingContext.toList()
                    )

                    // Notify any waiters for this transaction
                    notifyWaiters(txHashHex)

                    // Update outgoing context
                    spamManager.extractOutgoingInfo(fullTx, userAddress)?.let { outgoingInfo ->
                        outgoingContext.add(0, outgoingInfo)
                        if (outgoingContext.size > OUTGOING_CONTEXT_SIZE) {
                            outgoingContext.removeAt(outgoingContext.size - 1)
                        }
                    }
                }

                lastTxHash = transactions.lastOrNull()?.transaction?.hash

                // Update progress
                lastTxHash?.toHexString()?.let {
                    scannedTransactionStorage.save(
                        SpamScanState(source.blockchain.type, source.account.id, it)
                    )
                }

                if (transactions.size < BATCH_SIZE) break
            }

            // Mark as complete
            scannedTransactionStorage.save(
                SpamScanState(source.blockchain.type, source.account.id, SCAN_COMPLETE_MARKER)
            )

            // Notify all remaining waiters (transaction might not exist in blockchain data)
            notifyAllRemainingWaiters()

        } catch (_: Throwable) {
            // Error during scan - notify waiters so they don't hang forever
            notifyAllRemainingWaiters()
        }
    }

    private suspend fun rescanStellarTransactions(
        source: TransactionSource,
        adapter: io.horizontalsystems.bankwallet.core.adapters.StellarTransactionsAdapter
    ) {
        val selfAddress = adapter.stellarKitWrapper.stellarKit.receiveAddress
        val baseToken = spamManager.getBaseToken(source) ?: return

        try {
            var lastOperationId: Long? = null
            val outgoingContext = mutableListOf<PoisoningScorer.OutgoingTxInfo>()

            while (true) {
                val operations = adapter.getStellarOperationsBefore(lastOperationId, BATCH_SIZE)
                if (operations.isEmpty()) break

                // Sort by timestamp (oldest first) for correct outgoing context
                val sortedOperations = operations.sortedBy { it.timestamp }

                for (operation in sortedOperations) {
                    val txHashHex = operation.transactionHash

                    // Process the operation
                    spamManager.processStellarOperationForSpamWithContext(
                        operation,
                        source,
                        selfAddress,
                        baseToken,
                        outgoingContext.toList()
                    )

                    // Notify any waiters for this transaction
                    notifyWaiters(txHashHex)

                    // Update outgoing context
                    spamManager.extractOutgoingInfo(operation, selfAddress)?.let { outgoingInfo ->
                        outgoingContext.add(0, outgoingInfo)
                        if (outgoingContext.size > OUTGOING_CONTEXT_SIZE) {
                            outgoingContext.removeAt(outgoingContext.size - 1)
                        }
                    }
                }

                lastOperationId = operations.lastOrNull()?.id

                // Update progress
                lastOperationId?.toString()?.let {
                    scannedTransactionStorage.save(
                        SpamScanState(source.blockchain.type, source.account.id, it)
                    )
                }

                if (operations.size < BATCH_SIZE) break
            }

            // Mark as complete
            scannedTransactionStorage.save(
                SpamScanState(source.blockchain.type, source.account.id, SCAN_COMPLETE_MARKER)
            )

            // Notify all remaining waiters (transaction might not exist in blockchain data)
            notifyAllRemainingWaiters()

        } catch (_: Throwable) {
            // Error during scan - notify waiters so they don't hang forever
            notifyAllRemainingWaiters()
        }
    }

    private suspend fun notifyWaiters(txHashHex: String) {
        waitersMutex.withLock {
            pendingWaiters.remove(txHashHex)?.forEach { deferred ->
                deferred.complete(Unit)
            }
        }
    }

    private suspend fun notifyAllRemainingWaiters() {
        waitersMutex.withLock {
            pendingWaiters.values.flatten().forEach { deferred ->
                deferred.complete(Unit)
            }
            pendingWaiters.clear()
        }
    }
}