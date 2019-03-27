package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class RateManager(private val storage: IRateStorage, private val networkManager: INetworkManager) {

    private var refreshDisposables: CompositeDisposable = CompositeDisposable()

    fun refreshLatestRates(coinCodes: List<String>, currencyCode: String) {
        refreshDisposables.clear()

        refreshDisposables.add(
                networkManager.getLatestRateData(currencyCode)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe({ latestRateData ->
                            coinCodes.forEach { coinCode ->
                                latestRateData.rates[coinCode]?.toBigDecimalOrNull()?.let {
                                    val rate = Rate(coinCode, latestRateData.currency, it, latestRateData.timestamp, true)
                                    storage.saveLatest(rate)
                                }
                            }
                        }, {

                        }))
    }

    private fun getLatestRateFallbackFlowable(coinCode: CoinCode, currencyCode: String, timestamp: Long): Maybe<BigDecimal> {
        if (timestamp > ((System.currentTimeMillis() / 1000) - 3600)) {
            return Maybe.empty()
        }

        return storage.latestRateObservable(coinCode, currencyCode)
                .firstElement()
                .flatMap {
                    if (it.expired) {
                         Maybe.empty()
                    } else {
                        Maybe.just(it.value)
                    }
                }
    }

    fun rateValueObservable(coinCode: CoinCode, currencyCode: String, timestamp: Long): Maybe<BigDecimal> {
        return storage.rateMaybe(coinCode, currencyCode, timestamp)
                .map { it.value }
                .switchIfEmpty (
                    networkManager.getRate(coinCode, currencyCode, timestamp)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess { rateFromNetwork ->
                                storage.save(Rate(coinCode, currencyCode, rateFromNetwork, timestamp, false))
                            }
                            .switchIfEmpty(getLatestRateFallbackFlowable(coinCode, currencyCode, timestamp))
                )
    }

    fun clear() {
        storage.deleteAll()
    }

}
