package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class WalletManager(
        private val accountManager: IAccountManager,
        private val storage: IWalletStorage
) : IWalletManager {

    override val activeWallets get() = cachedActiveWallets.walletsSet.toList()
    override val activeWalletsUpdatedObservable = PublishSubject.create<List<Wallet>>()

    private val cachedActiveWallets = WalletsCache()

    private val disposables = CompositeDisposable()

    init {
        accountManager.accountsDeletedFlowable
                .subscribeIO {
                    loadWallets()
                }
                .let {
                    disposables.add(it)
                }

        accountManager.activeAccountObservable
                .subscribeIO {
                    val account = it.orElseGet(null)
                    handleUpdated(account)
                }
                .let {
                    disposables.add(it)
                }
    }

    override fun loadWallets() {
        val activeWallets = accountManager.activeAccount?.let { storage.wallets(it) } ?: listOf()

        cachedActiveWallets.set(activeWallets)
        notifyActiveWallets()
    }

    override fun save(wallets: List<Wallet>) {
        handle(wallets, listOf())
    }

    override fun delete(wallets: List<Wallet>) {
        handle(listOf(), wallets)
    }

    override fun handle(newWallets: List<Wallet>, deletedWallets: List<Wallet>) {
        storage.save(newWallets)
        storage.delete(deletedWallets)

        val activeAccount = accountManager.activeAccount
        cachedActiveWallets.add(newWallets.filter { it.account == activeAccount })
        cachedActiveWallets.remove(deletedWallets)
        notifyActiveWallets()
    }

    override fun getWallets(account: Account): List<Wallet> {
        return storage.wallets(account)
    }

    override fun clear() {
        storage.clear()
        cachedActiveWallets.clear()
        notifyActiveWallets()
        disposables.dispose()
    }

    private fun notifyActiveWallets() {
        activeWalletsUpdatedObservable.onNext(cachedActiveWallets.walletsSet.toList())
    }

    private fun handleUpdated(activeAccount: Account?) {
        val activeWallets = activeAccount?.let { storage.wallets(it) } ?: listOf()

        cachedActiveWallets.set(activeWallets)
        notifyActiveWallets()
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
