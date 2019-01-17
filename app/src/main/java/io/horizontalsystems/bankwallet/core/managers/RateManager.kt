package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class RateManager(private val storage: IRateStorage, private val networkManager: INetworkManager) {

    private var disposables: CompositeDisposable = CompositeDisposable()
    private var refreshDisposables: CompositeDisposable = CompositeDisposable()

    init {
        disposables.add(storage.zeroRatesObservables()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { rates ->
                    rates.forEach { rate ->
                        disposables.add(networkManager.getRate(rate.coinCode, rate.currencyCode, rate.timestamp)
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                                .subscribe { rateValue ->
                                    storage.save(Rate(rate.coinCode, rate.currencyCode, rateValue, rate.timestamp, false))
                                })
                    }
                }
        )
    }

    fun refreshLatestRates(coinCodes: List<String>, currencyCode: String) {
        refreshDisposables.clear()

        refreshDisposables.add(Flowable.mergeDelayError(
                coinCodes.map { coinCode ->
                    networkManager.getLatestRate(coinCode, currencyCode)
                            .map {
                                Rate(coinCode, currencyCode, it.value, it.timestamp, true)
                            }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    storage.save(it)
                }, {

                }))
    }

    fun rateValueObservable(coinCode: CoinCode, currencyCode: String, timestamp: Long): Flowable<Double> {
        return storage.rateObservable(coinCode, currencyCode, timestamp)
                .flatMap {
                    val rate = it.firstOrNull()

                    if (rate == null) {
                        storage.save(Rate(coinCode, currencyCode, 0.0, timestamp, false))
                    }

                    if (rate == null || rate.value == 0.0) {
                        Flowable.empty()
                    } else {
                        Flowable.just(rate.value)
                    }
                }
                .distinctUntilChanged()
    }
}
