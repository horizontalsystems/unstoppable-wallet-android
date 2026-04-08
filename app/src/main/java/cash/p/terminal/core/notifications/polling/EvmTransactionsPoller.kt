package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class EvmTransactionsPoller(
    private val evmBlockchainManager: EvmBlockchainManager,
    private val transactionAdapterManager: TransactionAdapterManager,
) : TransactionsPoller {

    override val blockchainTypes: Set<BlockchainType>
        get() = evmBlockchainManager.allBlockchains.map { it.type }.toSet()

    override suspend fun pollOnce(wallets: List<Wallet>): List<TransactionRecord> = coroutineScope {
        wallets.groupBy { it.token.blockchainType }.map { (blockchainType, chainWallets) ->
            async {
                val kitManager = evmBlockchainManager.getEvmKitManager(blockchainType)

                withTimeoutOrNull(TransactionsPoller.POLLING_TIMEOUT_MS) {
                    kitManager.startForPolling()
                    try {
                        awaitSyncAndRead(chainWallets, transactionAdapterManager)
                    } finally {
                        kitManager.stopForPolling()
                    }
                } ?: emptyList<TransactionRecord>().also {
                    Timber.tag("TxPoller").w("EVM(%s) poll timed out", blockchainType.uid)
                }
            }
        }.awaitAll().flatten()
    }
}
