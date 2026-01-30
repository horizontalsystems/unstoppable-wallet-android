package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.storage.ScannedTransactionStorage
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpamRescanManager(
    private val scannedTransactionStorage: ScannedTransactionStorage,
    private val spamManager: SpamManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val BATCH_SIZE = 50
        private const val OUTGOING_CONTEXT_SIZE = 10
        private const val SCAN_COMPLETE_MARKER = "complete"
    }

    /**
     * Start listening for adapter changes and run rescan when needed.
     * This handles both initial app startup and account switching.
     * When user switches to a different account, adaptersReadyFlow emits
     * the new account's adapters, and we check if rescan is needed.
     */
    fun start(transactionAdapterManager: TransactionAdapterManager) {
        coroutineScope.launch {
            transactionAdapterManager.adaptersReadyFlow.collect { adaptersMap ->
                checkAndRescanIfNeeded(adaptersMap)
            }
        }
    }

    private suspend fun checkAndRescanIfNeeded(adaptersMap: Map<TransactionSource, ITransactionsAdapter>) {
        val evmSources = adaptersMap.filter { (source, _) ->
            isEvmBlockchain(source)
        }

        evmSources.forEach { (source, adapter) ->
            val scanState = scannedTransactionStorage.getSpamScanState(
                source.blockchain.type,
                source.account.id
            )
            // Only skip if scan was completed (marked with "complete")
            // If scan was interrupted (has partial lastTxId), we restart from beginning
            val isCompleted = scanState?.lastSyncedTransactionId == SCAN_COMPLETE_MARKER
            if (!isCompleted) {
                rescanTransactionsForSource(source, adapter)
            }
        }
    }

    private suspend fun rescanTransactionsForSource(
        source: TransactionSource,
        adapter: ITransactionsAdapter
    ) {
        try {
            var lastTxId: String? = null

            while (true) {
                val transactions = adapter.getTransactionsAfter(lastTxId)
                if (transactions.isEmpty()) break

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

                lastTxId = transactions.lastOrNull()?.uid

                // Update progress
                lastTxId?.let {
                    scannedTransactionStorage.save(
                        SpamScanState(source.blockchain.type, source.account.id, it)
                    )
                }

                // If we got fewer transactions than batch size, we're done
                if (transactions.size < BATCH_SIZE) break
            }

            // Mark as complete
            scannedTransactionStorage.save(
                SpamScanState(source.blockchain.type, source.account.id, SCAN_COMPLETE_MARKER)
            )
        } catch (_: Throwable) {
            // Ignore errors during rescan - will retry on next app start
        }
    }

    private fun isEvmBlockchain(source: TransactionSource): Boolean {
        return EvmBlockchainManager.blockchainTypes.contains(source.blockchain.type)
    }
}
