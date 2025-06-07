package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.EnabledWallet
import io.reactivex.Observable

interface IWalletManager {
    val activeWallets: List<Wallet>
    val activeWalletsUpdatedObservable: Observable<List<Wallet>>

    fun save(wallets: List<Wallet>)
    suspend fun saveSuspended(wallets: List<Wallet>)
    fun saveEnabledWallets(enabledWallets: List<EnabledWallet>)
    fun delete(wallets: List<Wallet>)
    fun clear()
    fun handle(newWallets: List<Wallet>, deletedWallets: List<Wallet>)
    fun getWallets(account: Account): List<Wallet>
}