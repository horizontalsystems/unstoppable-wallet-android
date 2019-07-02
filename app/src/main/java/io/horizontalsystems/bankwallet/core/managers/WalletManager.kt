package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.Wallet
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.jetbrains.anko.collections.forEachWithIndex

class WalletManager(private val appConfigProvider: IAppConfigProvider, accountManager: AccountManager, private val walletStorage: IEnabledWalletStorage) : IWalletManager {

    private val disposables = CompositeDisposable()

    override val walletsUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()

    init {
        walletStorage.enabledWallets()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { enabledWallet ->
                    val accounts = accountManager.accounts
                    val enabledWallets = mutableListOf<Wallet>()

                    enabledWallet.forEach { wallet ->
                        val coin = appConfigProvider.coins.find { it.code == wallet.coinCode }
                        val account = accounts.find { it.name == wallet.accountName }

                        if (coin != null && account != null) {
                            enabledWallets.add(Wallet(coin, account, wallet.syncMode))
                        }
                    }

                    wallets = enabledWallets
                }
                .let { disposables.add(it) }

        accountManager.accountsFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { accounts ->
                    val enabledWallets = mutableListOf<EnabledWallet>()

                    val wallets = wallets.filter { accounts.contains(it.account) }
                    wallets.forEachWithIndex { i, wallet ->
                        enabledWallets.add(EnabledWallet(wallet.coin.code, i, wallet.account.name, wallet.syncMode))
                    }

                    this.wallets = wallets
                    walletStorage.save(enabledWallets)
                }
                .let { disposables.add(it) }
    }

    override var wallets: List<Wallet> = listOf()
        set(value) {
            field = value
            walletsUpdatedSignal.onNext(Unit)
        }

    override fun enableDefaultWallets() {
        // val enabledCoins = mutableListOf<EnabledWallet>()
        // appConfigProvider.defaultCoinCodes.forEachIndexed { order, coinCode ->
        //     enabledCoins.add(EnabledWallet(coinCode, order))
        // }
        // walletStorage.save(enabledCoins)
    }

    override fun clear() {
        wallets = listOf()
        walletStorage.deleteAll()
        disposables.clear()
    }
}
