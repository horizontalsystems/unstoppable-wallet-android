package cash.p.terminal.wallet

import io.reactivex.Flowable

interface IAdapterManager {
    val adaptersReadyObservable: Flowable<Map<Wallet, IAdapter>>
    fun startAdapterManager()
    suspend fun refresh()
    fun getAdapterForWallet(wallet: Wallet): IAdapter?
    fun getAdapterForToken(token: Token): IAdapter?
    fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter?
    fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter?
    fun refreshAdapters(wallets: List<Wallet>)
    fun refreshByWallet(wallet: Wallet)
}