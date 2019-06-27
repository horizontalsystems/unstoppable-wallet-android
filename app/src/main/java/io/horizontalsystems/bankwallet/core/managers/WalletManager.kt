package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class WalletManager(private val appConfigProvider: IAppConfigProvider, private val enabledCoinStorage: IEnabledCoinStorage) : ICoinManager {

    override val walletsUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()

    init {
        val accounts = listOf<Account>()
        val disposable = enabledCoinStorage.enabledCoinsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { enabledCoinsFromDb ->

                    val enabledWallets = mutableListOf<Wallet>()
                    enabledCoinsFromDb.forEach { enabledCoin ->
                        val coin = appConfigProvider.coins.find { coin -> coin.code == enabledCoin.coinCode }
                        val account = accounts.find {
                            it.name == enabledCoin.accountName
                        }

                        if (coin != null && account != null) {
                            enabledWallets.add(Wallet(coin, account))
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
        val enabledCoins = mutableListOf<EnabledWallet>()
        appConfigProvider.defaultCoinCodes.forEachIndexed { order, coinCode ->
            enabledCoins.add(EnabledWallet(coinCode, order))
        }
        enabledCoinStorage.save(enabledCoins)
    }

    override fun clear() {
        wallets = listOf()
        enabledCoinStorage.deleteAll()
    }
}
