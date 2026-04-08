package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.managers.SolanaKitManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Solana polling follows the same pause/resume pattern as EVM/Ton/Tron/Stellar/Monero:
 * the kit is paused in background and briefly resumed for each poll cycle.
 *
 * Race handling: the observer of the sync state flow is started BEFORE
 * [SolanaKitManager.startForPolling] via `async(UNDISPATCHED)`, so the
 * subscription is active when `refresh()` triggers the Syncing → Synced
 * transition and we don't read a stale DB cache.
 */
class SolanaTransactionsPoller(
    private val solanaKitManager: SolanaKitManager,
    private val transactionAdapterManager: TransactionAdapterManager,
) : TransactionsPoller {

    override val blockchainTypes = setOf(BlockchainType.Solana)

    override suspend fun pollOnce(wallets: List<Wallet>): List<TransactionRecord> {
        if (solanaKitManager.solanaKitWrapper == null) {
            Timber.tag("TxPoller").w("Solana wrapper is null, skipping poll")
            return emptyList()
        }

        return withTimeoutOrNull(TransactionsPoller.POLLING_TIMEOUT_MS) {
            coroutineScope {
                // Subscribe BEFORE startForPolling triggers refresh, so the
                // observer is active when the Syncing → Synced transition happens.
                val freshSync = async(start = CoroutineStart.UNDISPATCHED) {
                    solanaKitManager.awaitFreshSync()
                }
                solanaKitManager.startForPolling()
                try {
                    freshSync.await()
                    awaitSyncAndRead(wallets, transactionAdapterManager)
                } finally {
                    solanaKitManager.stopForPolling()
                }
            }
        } ?: emptyList<TransactionRecord>().also {
            Timber.tag("TxPoller").w("Solana poll timed out")
        }
    }
}
