package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.reactive.asFlow

interface TransactionsPoller {
    val blockchainTypes: Set<BlockchainType>
    suspend fun pollOnce(wallets: List<Wallet>): List<TransactionRecord>

    companion object {
        const val POLLING_TIMEOUT_MS = 60_000L
    }
}

suspend fun awaitAdapterSync(adapter: ITransactionsAdapter) {
    adapter.transactionsStateUpdatedFlowable
        .asFlow()
        .onStart { emit(Unit) }
        .first { adapter.transactionsState is AdapterState.Synced }
}

suspend fun awaitSyncAndRead(
    wallets: List<Wallet>,
    transactionAdapterManager: TransactionAdapterManager,
): List<TransactionRecord> {
    return wallets.flatMap { wallet ->
        val adapter = transactionAdapterManager.adaptersReadyFlow.value[wallet.transactionSource]
            ?: return@flatMap emptyList()
        awaitAdapterSync(adapter)
        adapter.getTransactions(
            from = null,
            token = null,
            limit = 100,
            transactionType = FilterTransactionType.All,
            address = null,
        )
    }
}
