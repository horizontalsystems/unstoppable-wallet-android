package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.Wallet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class WalletManager(private val appConfigProvider: IAppConfigProvider, accountManager: AccountManager, private val walletStorage: IEnabledWalletStorage) : IWalletManager {

    override val walletsUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()

    init {
        val disposable = walletStorage.enabledCoinsObservable()
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
    }
}
