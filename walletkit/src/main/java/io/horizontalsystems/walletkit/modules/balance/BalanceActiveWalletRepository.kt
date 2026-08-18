package io.horizontalsystems.walletkit.modules.balance

import io.horizontalsystems.walletkit.core.managers.EvmSyncSourceManager
import io.horizontalsystems.walletkit.core.managers.WalletManager
import io.horizontalsystems.walletkit.entities.Wallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class BalanceActiveWalletRepository(
    private val walletManager: WalletManager,
    evmSyncSourceManager: EvmSyncSourceManager
) {

    val itemsFlow: Flow<List<Wallet>> =
        merge(
            flowOf(Unit),
            walletManager.activeWalletsUpdatedFlow,
            evmSyncSourceManager.syncSourceFlow
        )
            .map {
                walletManager.activeWallets
            }

    fun disable(wallet: Wallet) {
        walletManager.delete(listOf(wallet))
    }

    fun enable(wallet: Wallet) {
        walletManager.save(listOf(wallet))
    }

}
