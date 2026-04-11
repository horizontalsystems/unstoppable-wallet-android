package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.managers.TonKitManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class TonTransactionsPoller(
    private val tonKitManager: TonKitManager,
    private val transactionAdapterManager: TransactionAdapterManager,
) : TransactionsPoller {

    override val blockchainTypes = setOf(BlockchainType.Ton)

    override suspend fun pollOnce(wallets: List<Wallet>): List<TransactionRecord> {
        return withTimeoutOrNull(TransactionsPoller.POLLING_TIMEOUT_MS) {
            tonKitManager.startForPolling()
            try {
                awaitSyncAndRead(wallets, transactionAdapterManager)
            } finally {
                tonKitManager.stopForPolling()
            }
        } ?: emptyList<TransactionRecord>().also {
            Timber.tag("TxPoller").w("TON poll timed out")
        }
    }
}
