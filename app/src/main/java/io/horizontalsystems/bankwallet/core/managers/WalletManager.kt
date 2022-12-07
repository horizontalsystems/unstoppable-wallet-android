package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class WalletManager(
    private val accountManager: IAccountManager,
    private val storage: IWalletStorage,
    testnetManager: EvmTestnetManager
) : IWalletManager {

    override val activeWallets get() = walletsSet.toList()
    override val activeWalletsUpdatedObservable = PublishSubject.create<List<Wallet>>()

    private val walletsSet = mutableSetOf<Wallet>()
    private val disposables = CompositeDisposable()

    init {
        accountManager.activeAccountObservable
            .subscribeIO {
                handleUpdated(it.orElse(null))
            }
            .let {
                disposables.add(it)
            }

        testnetManager.testnetUpdatedSignal
            .subscribeIO {
                handleTestnetUpdated(it)
            }
            .let {
                disposables.add(it)
            }
    }

    override fun loadWallets() {
        val activeWallets = accountManager.activeAccount?.let { storage.wallets(it) } ?: listOf()

        setWallets(activeWallets)
        notifyActiveWallets()
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
        disposables.dispose()
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

    @Synchronized
    private fun handleTestnetUpdated(isEnabled: Boolean) {
        if (isEnabled) {
            return
        }

        val deletedWallets = accountManager.accounts.map { account ->
            storage.wallets(account).filter {
                it.token.blockchainType is BlockchainType.EthereumGoerli
            }
        }

        handle(listOf(), deletedWallets.flatten())
    }

    override fun saveEnabledWallets(enabledWallets: List<EnabledWallet>) {
        storage.handle(enabledWallets)
        loadWallets()
    }

}
