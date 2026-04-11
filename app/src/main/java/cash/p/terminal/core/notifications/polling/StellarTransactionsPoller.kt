package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.managers.StellarKitManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class StellarTransactionsPoller(
    private val stellarKitManager: StellarKitManager,
    private val transactionAdapterManager: TransactionAdapterManager,
) : TransactionsPoller {

    override val blockchainTypes = setOf(BlockchainType.Stellar)

    override suspend fun pollOnce(wallets: List<Wallet>): List<TransactionRecord> {
        return withTimeoutOrNull(TransactionsPoller.POLLING_TIMEOUT_MS) {
            stellarKitManager.startForPolling()
            try {
                awaitSyncAndRead(wallets, transactionAdapterManager)
            } finally {
                stellarKitManager.stopForPolling()
            }
        } ?: emptyList<TransactionRecord>().also {
            Timber.tag("TxPoller").w("Stellar poll timed out")
        }
    }
}
