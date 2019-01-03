package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function4
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class RateSyncer(rateManager: RateManager,
                 walletManager: IWalletManager,
                 currencyManager: ICurrencyManager,
                 networkAvailabilityManager: NetworkAvailabilityManager,
                 timer: Flowable<Long> = Flowable.interval(0L, 3L, TimeUnit.MINUTES)) {

    private val disposables = CompositeDisposable()

    init {
        disposables.add(Flowable.combineLatest(
                walletManager.walletsObservable,
                currencyManager.baseCurrencyObservable,
                networkAvailabilityManager.stateObservable,
                timer,
                Function4<List<Wallet>, Currency, Boolean, Long, Triple<List<Wallet>, Currency, Boolean>> { t1, t2, t3, _ ->
                    Triple(t1, t2, t3)
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { (wallets, currency, networkConnected) ->
                    if (networkConnected) {
                        rateManager.refreshRates(wallets.map { it.coinCode }, currency.code)
                    }
                })
    }
}