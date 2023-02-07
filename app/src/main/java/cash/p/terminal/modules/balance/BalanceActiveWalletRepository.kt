package cash.p.terminal.modules.balance

import cash.p.terminal.core.IWalletManager
import cash.p.terminal.core.managers.EvmSyncSourceManager
import cash.p.terminal.entities.Wallet
import io.reactivex.Observable

class BalanceActiveWalletRepository(
    private val walletManager: IWalletManager,
    evmSyncSourceManager: EvmSyncSourceManager
) {

    val itemsObservable: Observable<List<Wallet>> =
        Observable
            .merge(
                Observable.just(Unit),
                walletManager.activeWalletsUpdatedObservable,
                evmSyncSourceManager.syncSourceObservable
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
