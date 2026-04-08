package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.managers.MoneroKitManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class MoneroTransactionsPoller(
    private val moneroKitManager: MoneroKitManager,
    private val transactionAdapterManager: TransactionAdapterManager,
) : TransactionsPoller {

    override val blockchainTypes = setOf(BlockchainType.Monero)

    override suspend fun pollOnce(wallets: List<Wallet>): List<TransactionRecord> {
        val wrapper = moneroKitManager.moneroKitWrapper
        if (wrapper == null) {
            Timber.tag("TxPoller").w("Monero wrapper is null, skipping poll")
            return emptyList()
        }

        return withTimeoutOrNull(TransactionsPoller.POLLING_TIMEOUT_MS) {
            moneroKitManager.startForPolling()
            try {
                wrapper.syncState.first { it is AdapterState.Synced }
                readTransactions(wallets)
            } finally {
                moneroKitManager.stopForPolling()
            }
        } ?: emptyList<TransactionRecord>().also {
            Timber.tag("TxPoller").w("Monero poll timed out")
        }
    }

    private suspend fun readTransactions(wallets: List<Wallet>): List<TransactionRecord> {
        return wallets.flatMap { wallet ->
            val adapter = transactionAdapterManager.adaptersReadyFlow.value[wallet.transactionSource]
            adapter?.getTransactions(
                from = null,
                token = null,
                limit = 100,
                transactionType = FilterTransactionType.All,
                address = null,
            ) ?: emptyList()
        }
    }
}
