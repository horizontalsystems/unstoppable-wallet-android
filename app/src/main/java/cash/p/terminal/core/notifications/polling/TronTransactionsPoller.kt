package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.managers.TronKitManager
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class TronTransactionsPoller(
    private val tronKitManager: TronKitManager,
    private val transactionAdapterManager: TransactionAdapterManager,
) : TransactionsPoller {

    override val blockchainTypes = setOf(BlockchainType.Tron)

    override suspend fun pollOnce(wallets: List<Wallet>): List<TransactionRecord> {
        return withTimeoutOrNull(TransactionsPoller.POLLING_TIMEOUT_MS) {
            tronKitManager.startForPolling()
            try {
                awaitSyncAndRead(wallets, transactionAdapterManager)
            } finally {
                tronKitManager.stopForPolling()
            }
        } ?: emptyList<TransactionRecord>().also {
            Timber.tag("TxPoller").w("Tron poll timed out")
        }
    }
}
