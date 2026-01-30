package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.storage.ScannedTransactionStorage
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    }

    fun runRescanIfNeeded(transactionAdapterManager: TransactionAdapterManager) {
        Timber.tag(TAG).d("Starting spam rescan check...")
        coroutineScope.launch {
            val evmSources = transactionAdapterManager.adaptersMap.filter { (source, _) ->
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

                Timber.tag(TAG).d("[$blockchainName] Processing batch #$batchNumber: ${transactions.size} transactions")

                transactions.forEach { tx ->
                    spamManager.processTransactionForSpam(tx, source)
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
