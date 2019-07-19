package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class RateSyncer(private val rateManager: RateManager,
                 private val adapterManager: IAdapterManager,
                 private val currencyManager: ICurrencyManager,
                 private val networkAvailabilityManager: NetworkAvailabilityManager,
                 timerSignal: Observable<Unit> = Observable.interval(0L, 3L, TimeUnit.MINUTES).map { Unit }) {

    private val disposables = CompositeDisposable()

    init {
        disposables.add(Observable.merge(
                adapterManager.adaptersUpdatedSignal,
                currencyManager.baseCurrencyUpdatedSignal,
                networkAvailabilityManager.networkAvailabilitySignal,
                timerSignal)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    requestRefresh()
                })
    }

    private fun requestRefresh() {
        if (networkAvailabilityManager.isConnected) {
            val coinCodes = mutableSetOf<CoinCode>()
            adapterManager.adapters.forEach {
                coinCodes.add(it.wallet.coin.code)
                it.feeCoinCode?.let { feeCoinCode ->
                    coinCodes.add(feeCoinCode)
                }
            }

            rateManager.refreshLatestRates(coinCodes.toList(), currencyManager.baseCurrency.code)
        }
    }
}
