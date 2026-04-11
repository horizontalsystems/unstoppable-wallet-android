package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.adapters.BitcoinBaseAdapter
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class BtcLikeTransactionsPoller(
    private val btcBlockchainManager: BtcBlockchainManager,
    private val transactionAdapterManager: TransactionAdapterManager,
) : TransactionsPoller {

    override val blockchainTypes: Set<BlockchainType>
        get() = btcBlockchainManager.allBlockchains.map { it.type }.toSet()

    override suspend fun pollOnce(wallets: List<Wallet>): List<TransactionRecord> = coroutineScope {
        wallets.map { wallet ->
            async {
                val adapter = transactionAdapterManager.adaptersReadyFlow.value[wallet.transactionSource]
                    ?: return@async emptyList<TransactionRecord>()
                val btcAdapter = adapter as? BitcoinBaseAdapter
                    ?: return@async emptyList<TransactionRecord>()

                withTimeoutOrNull(TransactionsPoller.POLLING_TIMEOUT_MS) {
                    btcAdapter.startForPolling()
                    try {
                        awaitAdapterSync(adapter)
                        adapter.getTransactions(
                            from = null,
                            token = null,
                            limit = 100,
                            transactionType = FilterTransactionType.All,
                            address = null,
                        )
                    } finally {
                        btcAdapter.stopForPolling()
                    }
                } ?: emptyList<TransactionRecord>().also {
                    Timber.tag("TxPoller").w("BTC(%s) poll timed out", wallet.token.blockchainType.uid)
                }
            }
        }.awaitAll().flatten()
    }
}
