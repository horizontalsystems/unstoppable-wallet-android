package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletFactory
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.subjects.PublishSubject

class WalletManager(private val accountManager: IAccountManager, private val walletFactory: IWalletFactory, private val storage: IWalletStorage)
    : IWalletManager {

    override val wallets get() = cache.walletsSet.toList()
    override val walletsUpdatedObservable = PublishSubject.create<List<Wallet>>()

    private val cache = WalletsCache()
    private val disposable = accountManager.accountsFlowable.subscribe {
        loadWallets()
    }

    override fun wallet(coin: Coin): Wallet? {
        val account = accountManager.account(coin.type) ?: return null

        return walletFactory.wallet(coin, account, account.defaultSyncMode)
    }

    override fun loadWallets() {
        val wallets = storage.wallets(accountManager.accounts)
        cache.set(wallets)
        walletsUpdatedObservable.onNext(wallets)
    }

    override fun enable(wallets: List<Wallet>) {
        storage.save(wallets)
        cache.set(wallets)
        walletsUpdatedObservable.onNext(wallets)
    }

    override fun clear() {
        cache.clear()
        disposable.dispose()
    }

    private class WalletsCache {
        var walletsSet = mutableSetOf<Wallet>()
            private set

        fun set(wallets: List<Wallet>) {
            walletsSet = wallets.toMutableSet()
        }

        fun clear() {
            walletsSet.clear()
        }
    }
}
