package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class WalletManager(
        private val appConfigProvider: IAppConfigProvider,
        accountManager: IAccountManager,
        private val walletStorage: IEnabledWalletStorage)
    : IWalletManager {

    private val disposables = CompositeDisposable()

    override val walletsUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()

    init {
        walletStorage.enabledWallets()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { enabledWallets ->
                    val wallets = mutableListOf<Wallet>()

                    enabledWallets.forEach { wallet ->
                        val coin = appConfigProvider.coins.find { it.code == wallet.coinCode }
                        val account = accountManager.accounts.find { it.name == wallet.accountId }

                        if (coin != null && account != null) {
                            wallets.add(Wallet(coin, account, wallet.syncMode))
                        }
                    }

                    this.wallets = wallets
                }
                .let { disposables.add(it) }

        accountManager.accountsFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { accounts ->
                    enable(wallets.filter { accounts.contains(it.account) })
                }
                .let { disposables.add(it) }
    }

    override var wallets: List<Wallet> = listOf()
        set(value) {
            field = value
            walletsUpdatedSignal.onNext(Unit)
        }

    override fun enable(wallets: List<Wallet>) {
        val enabledWallets = wallets.mapIndexed { order, wallet ->
            EnabledWallet(wallet.coin.code, wallet.account.id, order, wallet.syncMode)
        }

        this.wallets = wallets
        walletStorage.save(enabledWallets)
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
