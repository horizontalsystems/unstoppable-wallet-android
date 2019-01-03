package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.LatestRate
import io.horizontalsystems.bankwallet.entities.Rate
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class RateManager(private val storage: IRateStorage, private val networkManager: INetworkManager) {

    val latestRates = mutableMapOf<String, MutableMap<String, LatestRate>>()

    private var refreshDisposables: CompositeDisposable = CompositeDisposable()

    fun rate(coin: String, currencyCode: String): Maybe<Rate> {
        return storage.rate(coin, currencyCode)
    }

    fun refreshRates(coinCodes: List<String>, currencyCode: String) {
        refreshDisposables.clear()

        refreshDisposables.add(Flowable.mergeDelayError(
                coinCodes.map { coinCode ->
                    networkManager.getLatestRate(coinCode, currencyCode)
                            .map {
                                Rate(coinCode, currencyCode, it.value, it.timestamp)
                            }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    storage.save(it)
                }, {

                }))
    }
}
