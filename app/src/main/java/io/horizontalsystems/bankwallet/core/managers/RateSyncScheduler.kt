package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class RateSyncScheduler(private val rateManager: IRateManager,
                        walletManager: IWalletManager,
                        currencyManager: ICurrencyManager,
                        networkAvailabilityManager: NetworkAvailabilityManager,
                        timerSignal: Observable<Unit> = Observable.interval(0L, 5L, TimeUnit.MINUTES).map { Unit }) {

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
                    rateManager.syncLatestRates()
                })
    }

}
