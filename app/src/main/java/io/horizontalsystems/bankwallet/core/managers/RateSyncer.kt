package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class RateSyncer(private val rateManager: RateManager,
                 private val walletManager: IWalletManager,
                 private val currencyManager: ICurrencyManager,
                 private val networkAvailabilityManager: NetworkAvailabilityManager,
                 timerSignal: Observable<Unit> = Observable.interval(0L, 3L, TimeUnit.MINUTES).map { Unit }) {

    private val disposables = CompositeDisposable()

    init {
        disposables.add(Observable.merge(
                walletManager.walletsUpdatedSignal,
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
            rateManager.refreshRates(walletManager.wallets.map { it.coinCode }, currencyManager.baseCurrency.code)
        }
    }
}