package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IEnabledCoinStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.EnabledCoin
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ManageCoinsInteractor(private val coinManager: ICoinManager, private val enabledCoinStorage: IEnabledCoinStorage)
    : ManageCoinsModule.IInteractor {

    var delegate: ManageCoinsModule.IInteractorDelegate? = null
    private val disposables = CompositeDisposable()

    override fun loadCoins() {
        delegate?.didLoadAllCoins(coinManager.allCoins)

        disposables.add(enabledCoinStorage.enabledCoinsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { enabledCoinsFromDb ->
                    val enabledCoins = mutableListOf<Coin>()
                    enabledCoinsFromDb.forEach { enabledCoin ->
                        coinManager.allCoins
                                .firstOrNull { coin -> coin.code == enabledCoin.coinCode}?.let { enabledCoins.add(it) }
                    }

                    delegate?.didLoadEnabledCoins(enabledCoins)
                })
    }

    override fun saveEnabledCoins(coins: List<Coin>) {
        val enabledCoins = mutableListOf<EnabledCoin>()
        coins.forEachIndexed{order, coinCode ->
            enabledCoins.add(EnabledCoin(coinCode.code, order))
        }
        enabledCoinStorage.save(enabledCoins)
        delegate?.didSaveChanges()
    }

    override fun clear() {
        disposables.clear()
    }

}
