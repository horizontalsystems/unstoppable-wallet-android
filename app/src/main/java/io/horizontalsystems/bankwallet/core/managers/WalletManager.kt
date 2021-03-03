package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class WalletManager(private val accountManager: IAccountManager, private val storage: IWalletStorage)
    : IWalletManager {

    override val wallets get() = cache.walletsSet.toList()
    override val walletsUpdatedObservable = PublishSubject.create<List<Wallet>>()

    private val cache = WalletsCache()
    private val disposable = accountManager.accountsDeletedFlowable
            .observeOn(Schedulers.io())
            .subscribe {
                loadWallets()
            }

    override fun wallet(coin: Coin): Wallet? {
        val account = accountManager.account(coin.type) ?: return null
        return storage.wallet(account, coin)
    }

    override fun loadWallets() {
        val wallets = storage.wallets(accountManager.accounts)
        cache.set(wallets)
        notifyChange()
    }

    override fun enable(wallets: List<Wallet>) {
        storage.save(wallets)
        cache.set(wallets)
        notifyChange()
    }

    override fun save(wallets: List<Wallet>) {
        storage.save(wallets)
        cache.add(wallets)
        notifyChange()
    }

    override fun delete(wallets: List<Wallet>) {
        storage.delete(wallets)
        cache.remove(wallets)
        notifyChange()
    }

    override fun clear() {
        cache.clear()
        disposable.dispose()
    }

    private fun notifyChange() {
        walletsUpdatedObservable.onNext(cache.walletsSet.toList())
    }

    private class WalletsCache {
        var walletsSet = mutableSetOf<Wallet>()
            private set

        fun add(wallets: List<Wallet>) {
            walletsSet.addAll(wallets)
        }

        fun remove(wallets: List<Wallet>) {
            walletsSet.removeAll(wallets)
        }

        fun set(wallets: List<Wallet>) {
            walletsSet = wallets.toMutableSet()
        }

        fun clear() {
            walletsSet.clear()
        }
    }
}
