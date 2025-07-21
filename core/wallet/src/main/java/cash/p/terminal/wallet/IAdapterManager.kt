package cash.p.terminal.wallet

import io.reactivex.Flowable
import kotlinx.coroutines.flow.StateFlow

interface IAdapterManager {
    val adaptersReadyObservable: Flowable<Map<Wallet, IAdapter>>
    val initializationInProgressFlow: StateFlow<Boolean>

    fun startAdapterManager()
    suspend fun refresh()
    fun getAdapterForWallet(wallet: Wallet): IAdapter?
    fun <T : IAdapter> getAdapterForToken(token: Token): T?
    fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter?
    fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter?
    fun refreshAdapters(wallets: List<Wallet>)
    fun refreshByWallet(wallet: Wallet)
}