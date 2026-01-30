package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.storage.ScannedTransactionStorage
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class SpamRescanManager(
    private val scannedTransactionStorage: ScannedTransactionStorage,
    private val spamManager: SpamManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "SpamRescanManager"
        private const val BATCH_SIZE = 50
        private const val OUTGOING_CONTEXT_SIZE = 10
    }

    fun runRescanIfNeeded(transactionAdapterManager: TransactionAdapterManager) {
        Timber.tag(TAG).d("Waiting for adapters to be ready...")
        coroutineScope.launch {
            // Wait for adapters to be ready
            val adaptersMap = transactionAdapterManager.adaptersReadyFlow.first()
            Timber.tag(TAG).d("Adapters ready, starting spam rescan check...")

            val evmSources = adaptersMap.filter { (source, _) ->
                isEvmBlockchain(source)
            }
            Timber.tag(TAG).d("Found ${evmSources.size} EVM sources to check")

            evmSources.forEach { (source, adapter) ->
                val scanState = scannedTransactionStorage.getSpamScanState(
                    source.blockchain.type,
                    source.account.id
                )
                if (scanState == null) {
                    Timber.tag(TAG).d("Rescan needed for ${source.blockchain.name} (account: ${source.account.id.take(8)}...)")
                    rescanTransactionsForSource(source, adapter)
                } else {
                    Timber.tag(TAG).d("Rescan already done for ${source.blockchain.name} (lastTx: ${scanState.lastSyncedTransactionId.take(16)}...)")
                }
            }
            Timber.tag(TAG).d("Spam rescan check completed")
        }
    }

    private suspend fun rescanTransactionsForSource(
        source: TransactionSource,
        adapter: ITransactionsAdapter
    ) {
        val blockchainName = source.blockchain.name
        Timber.tag(TAG).d("[$blockchainName] Starting rescan...")

        try {
            var lastTxId: String? = null
            var processedCount = 0
            var batchNumber = 0

            while (true) {
                batchNumber++
                val transactions = adapter.getTransactionsAfter(lastTxId)
                if (transactions.isEmpty()) {
                    Timber.tag(TAG).d("[$blockchainName] No more transactions to process")
                    break
                }

                // Find the oldest transaction in this batch to use as reference point
                val oldestTxInBatch = transactions.minByOrNull { it.timestamp }

                // IMPORTANT: Fresh outgoing context for each batch (not using SpamManager's cache)
                // We fetch outgoing transactions that are OLDER than this batch's oldest transaction
                // This is required for proper poisoning detection - attacker mimics addresses
                // from outgoing txs that happened BEFORE the spam transaction
                val outgoingContext = oldestTxInBatch?.let { oldestTx ->
                    adapter.getTransactions(
                        from = oldestTx,
                        token = null,
                        limit = OUTGOING_CONTEXT_SIZE,
                        transactionType = FilterTransactionType.Outgoing,
                        address = null
                    ).mapNotNull { spamManager.extractOutgoingInfo(it) }
                }?.toMutableList() ?: mutableListOf()

                Timber.tag(TAG).d("[$blockchainName] Batch #$batchNumber: ${transactions.size} txs, initial context: ${outgoingContext.size} older outgoing txs")

                transactions.forEach { tx ->
                    // Process the transaction with current outgoing context
                    spamManager.processTransactionForSpamWithContext(tx, source, outgoingContext.toList())

                    // If this tx is outgoing, add it to context for subsequent txs in this batch
                    spamManager.extractOutgoingInfo(tx)?.let { outgoingInfo ->
                        outgoingContext.add(0, outgoingInfo)
                        // Keep context size limited
                        if (outgoingContext.size > OUTGOING_CONTEXT_SIZE) {
                            outgoingContext.removeAt(outgoingContext.size - 1)
                        }
                    }
                }

                processedCount += transactions.size
                lastTxId = transactions.lastOrNull()?.uid

                // Update progress
                lastTxId?.let {
                    scannedTransactionStorage.save(
                        SpamScanState(source.blockchain.type, source.account.id, it)
                    )
                }

                Timber.tag(TAG).d("[$blockchainName] Progress: $processedCount transactions processed")

                // If we got fewer transactions than batch size, we're done
                if (transactions.size < BATCH_SIZE) break
            }

            // Mark as complete even if no transactions were found
            if (lastTxId == null) {
                scannedTransactionStorage.save(
                    SpamScanState(source.blockchain.type, source.account.id, "complete")
                )
            }

            Timber.tag(TAG).d("[$blockchainName] Rescan completed. Total: $processedCount transactions")
        } catch (e: Throwable) {
            Timber.tag(TAG).e(e, "[$blockchainName] Rescan failed")
        }
    }

    private fun isEvmBlockchain(source: TransactionSource): Boolean {
        return EvmBlockchainManager.blockchainTypes.contains(source.blockchain.type)
    }
}
