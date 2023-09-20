package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WalletManager(
    private val accountManager: IAccountManager,
    private val storage: IWalletStorage,
) : IWalletManager {

    override val activeWallets get() = walletsSet.toList()
    override val activeWalletsUpdatedObservable = PublishSubject.create<List<Wallet>>()

    private val walletsSet = mutableSetOf<Wallet>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        coroutineScope.launch {
            accountManager.activeAccountStateFlow.collect { activeAccountState ->
                if (activeAccountState is ActiveAccountState.ActiveAccount) {
                    handleUpdated(activeAccountState.account)
                }
            }
        }
    }

    override fun save(wallets: List<Wallet>) {
        handle(wallets, listOf())
    }

    override fun delete(wallets: List<Wallet>) {
        handle(listOf(), wallets)
    }

    @Synchronized
    override fun handle(newWallets: List<Wallet>, deletedWallets: List<Wallet>) {
        storage.save(newWallets)
        storage.delete(deletedWallets)

        val activeAccount = accountManager.activeAccount
        walletsSet.addAll(newWallets.filter { it.account == activeAccount })
        walletsSet.removeAll(deletedWallets)
        notifyActiveWallets()
    }

    override fun getWallets(account: Account): List<Wallet> {
        return storage.wallets(account)
    }

    override fun clear() {
        storage.clear()
        walletsSet.clear()
        notifyActiveWallets()
        coroutineScope.cancel()
    }

    private fun notifyActiveWallets() {
        activeWalletsUpdatedObservable.onNext(walletsSet.toList())
    }

    @Synchronized
    private fun handleUpdated(activeAccount: Account?) {
        val activeWallets = activeAccount?.let { storage.wallets(it) } ?: listOf()

        setWallets(activeWallets)
        notifyActiveWallets()
    }

    @Synchronized
    private fun setWallets(activeWallets: List<Wallet>) {
        walletsSet.clear()
        walletsSet.addAll(activeWallets)
    }

    override fun saveEnabledWallets(enabledWallets: List<EnabledWallet>) {
        storage.handle(enabledWallets)
        handleUpdated(accountManager.activeAccount)
    }

}
