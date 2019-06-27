package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ManageCoinsInteractor(private val appConfigProvider: IAppConfigProvider, private val enabledCoinStorage: IEnabledWalletStorage)
    : ManageCoinsModule.IInteractor {

    var delegate: ManageCoinsModule.IInteractorDelegate? = null
    private val disposables = CompositeDisposable()

    override fun loadCoins() {
        delegate?.didLoadAllCoins(appConfigProvider.coins)

        disposables.add(enabledCoinStorage.enabledCoinsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { enabledCoinsFromDb ->
                    val enabledCoins = mutableListOf<Coin>()
                    enabledCoinsFromDb.forEach { enabledCoin ->
                        appConfigProvider.coins.firstOrNull { coin -> coin.code == enabledCoin.coinCode }?.let { enabledCoins.add(it) }
                    }

                    delegate?.didLoadEnabledCoins(enabledCoins)
                })
    }

    override fun saveEnabledCoins(coins: List<Coin>) {
        // val enabledCoins = mutableListOf<EnabledWallet>()
        // coins.forEachIndexed { order, coinCode ->
        //     enabledCoins.add(EnabledWallet(coinCode.code, order))
        // }
        // enabledCoinStorage.save(enabledCoins)
        // delegate?.didSaveChanges()
    }

    override fun clear() {
        disposables.clear()
    }

}
