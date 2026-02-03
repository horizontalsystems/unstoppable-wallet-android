package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.EvmTransactionsAdapter
import io.horizontalsystems.bankwallet.core.storage.ScannedTransactionStorage
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.toHexString
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
        // Get user address and base token from EVM adapter
        val evmAdapter = adapter as? EvmTransactionsAdapter ?: return
        val userAddress = evmAdapter.evmKitWrapper.evmKit.receiveAddress
        val baseToken = evmAdapter.baseToken

        try {
            var lastTxHash: ByteArray? = null
            val outgoingContext = mutableListOf<PoisoningScorer.OutgoingTxInfo>()

            while (true) {
                // Use getFullTransactionsBefore to avoid triggering isSpam() during conversion
                val transactions = adapter.getFullTransactionsBefore(lastTxHash, BATCH_SIZE)
                if (transactions.isEmpty()) break

                // Sort by timestamp (oldest first) to ensure correct outgoing context
                // Address poisoning detection needs context from OLDER outgoing transactions
                val sortedTransactions = transactions.sortedBy { it.transaction.timestamp }

                sortedTransactions.forEach { fullTx ->
                    // Process the transaction with current outgoing context
                    spamManager.processFullTransactionForSpamWithContext(
                        fullTx,
                        source,
                        userAddress,
                        baseToken,
                        outgoingContext.toList()
                    )

                    // If this tx is outgoing, add it to context for subsequent (newer) txs
                    spamManager.extractOutgoingInfoFromFullTransaction(fullTx, userAddress)?.let { outgoingInfo ->
                        outgoingContext.add(0, outgoingInfo)
                        // Keep context size limited
                        if (outgoingContext.size > OUTGOING_CONTEXT_SIZE) {
                            outgoingContext.removeAt(outgoingContext.size - 1)
                        }
                    }
                }

                // Use the last transaction from original order for pagination
                lastTxHash = transactions.lastOrNull()?.transaction?.hash

                // Update progress
                lastTxHash?.toHexString()?.let {
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
