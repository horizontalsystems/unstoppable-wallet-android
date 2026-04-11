package cash.p.terminal.core.notifications.polling

import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class TransactionPollingManager(
    private val pollers: List<TransactionsPoller>,
    private val backgroundManager: BackgroundManager,
) {
    suspend fun pollAll(
        blockchainTypes: Set<BlockchainType>,
        wallets: List<Wallet>,
    ): List<TransactionRecord> = coroutineScope {
        if (backgroundManager.inForeground) {
            Timber.tag("TxPoller").d("Polling skipped: app is in foreground")
            return@coroutineScope emptyList()
        }

        val relevantPollers = pollers.filter { poller -> poller.blockchainTypes.any { it in blockchainTypes } }

        relevantPollers.map { poller ->
            async {
                if (backgroundManager.inForeground) {
                    Timber.tag("TxPoller").d("Sub-polling stopped: app is in foreground")
                    return@async emptyList()
                }

                val blockchains = poller.blockchainTypes.joinToString(", ") { it.uid }
                val relevantWallets = wallets.filter { it.token.blockchainType in poller.blockchainTypes }
                Timber.tag("KeepAlive").d("Poll started: %s", blockchains)
                try {
                    val records = poller.pollOnce(relevantWallets)
                    Timber.tag("KeepAlive").d("Poll done: %s, %d transactions", blockchains, records.size)
                    records
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Timber.tag("TxPoller").e(e, "Poller %s failed", poller::class.simpleName)
                    emptyList()
                }
            }
        }.awaitAll().flatten()
    }
}
