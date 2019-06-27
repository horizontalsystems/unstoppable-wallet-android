package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.core.Wallet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class WalletManager(private val appConfigProvider: IAppConfigProvider, accountManager: AccountManager, private val enabledCoinStorage: IEnabledWalletStorage) : ICoinManager {

    override val walletsUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()

    init {
        val accounts = accountManager.accounts
        val disposable = enabledCoinStorage.enabledCoinsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { enabledCoinsFromDb ->

                    val enabledWallets = mutableListOf<Wallet>()
                    enabledCoinsFromDb.forEach { enabledWallet ->
                        val coin = appConfigProvider.coins.find { coin -> coin.code == enabledWallet.coinCode }
                        val account = accounts.find {
                            it.name == enabledWallet.accountName
                        }

                        if (coin != null && account != null) {
                            enabledWallets.add(Wallet(coin, account, enabledWallet.syncMode))
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
        // enabledCoinStorage.save(enabledCoins)
    }

    override fun clear() {
        wallets = listOf()
        enabledCoinStorage.deleteAll()
    }
}
