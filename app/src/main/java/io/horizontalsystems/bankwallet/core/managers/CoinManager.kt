package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ICoinStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class CoinManager(private val appConfigProvider: IAppConfigProvider, private val coinStorage: ICoinStorage) : ICoinManager {

    override val coinsUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()

    init {
        val disposable = coinStorage.enabledCoinsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    coins = it
                }
    }

    override var coins: List<Coin> = listOf()
        set(value) {
            field = value
            coinsUpdatedSignal.onNext(Unit)
        }

    override val allCoinsObservable: Flowable<List<Coin>>
        get() = coinStorage.allCoinsObservable().flatMap { all ->
            val defaults = appConfigProvider.defaultCoins
            val allCoins = all.filter { !defaults.contains(it) }

            Flowable.just(defaults + allCoins)
        }

    override fun enableDefaultCoins() {
        coinStorage.save(appConfigProvider.defaultCoins)
    }

    override fun clear() {
        coins = listOf()
        coinStorage.deleteAll()
    }
}
