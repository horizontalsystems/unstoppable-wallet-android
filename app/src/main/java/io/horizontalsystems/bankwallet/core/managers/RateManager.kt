package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.math.BigDecimal
import java.net.SocketTimeoutException

class RateManager(private val storage: IRateStorage, private val networkManager: INetworkManager) {

    private var refreshDisposables: CompositeDisposable = CompositeDisposable()

    fun refreshLatestRates(coinCodes: List<String>, currencyCode: String) {
        refreshDisposables.clear()

        //mainUrl sometime returns expired rates, thus currently first request is done to fallbackUrl
        refreshDisposables.add(
                networkManager.getLatestRateData(ServiceExchangeApi.HostType.FALLBACK, currencyCode)
                        .onErrorResumeNext(networkManager.getLatestRateData(ServiceExchangeApi.HostType.MAIN, currencyCode))
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe ({ latestRateData ->
                            coinCodes.forEach { coinCode ->
                                latestRateData.rates[coinCode]?.toBigDecimalOrNull()?.let {
                                    val rate = Rate(coinCode, latestRateData.currency, it, latestRateData.timestamp, true)
                                    storage.saveLatest(rate)
                                }
                            }
                        }, {
                            //request failed
                        })
        )
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
                .switchIfEmpty(
                        networkManager.getRateByHour(ServiceExchangeApi.HostType.MAIN, coinCode, currencyCode, timestamp)
                                .onErrorResumeNext { t: Throwable ->
                                    when (t) {
                                        is SocketTimeoutException ->
                                            networkManager.getRateByHour(ServiceExchangeApi.HostType.FALLBACK, coinCode, currencyCode, timestamp)
                                                    .onErrorResumeNext(networkManager.getRateByDay(ServiceExchangeApi.HostType.FALLBACK, coinCode, currencyCode, timestamp))
                                        is HttpException ->
                                            networkManager.getRateByDay(ServiceExchangeApi.HostType.MAIN, coinCode, currencyCode, timestamp)
                                                    .onErrorResumeNext(networkManager.getRateByDay(ServiceExchangeApi.HostType.FALLBACK, coinCode, currencyCode, timestamp))
                                        else -> throw t
                                    }
                                }
                                .doOnSuccess { rateFromNetwork ->
                                    storage.save(Rate(coinCode, currencyCode, rateFromNetwork, timestamp, false))
                                }
                                .onErrorResumeNext(getLatestRateFallbackFlowable(coinCode, currencyCode, timestamp))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                )
    }

    fun clear() {
        storage.deleteAll()
    }

}
