package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.LatestRate
import io.horizontalsystems.bankwallet.entities.Rate
import io.reactivex.Maybe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class RateManager(
        private val storage: IRateStorage,
        private val syncer: RateSyncer,
        private val walletManager: IWalletManager,
        private val currencyManager: ICurrencyManager,
        networkAvailabilityManager: NetworkAvailabilityManager,
        timer: PeriodicTimer) : IPeriodicTimerDelegate, IRateSyncerDelegate {

    val subject: PublishSubject<Boolean> = PublishSubject.create()
    private var disposables: CompositeDisposable = CompositeDisposable()

    init {
        timer.delegate = this

        disposables.add(walletManager.walletsSubject.subscribe {
            updateRates()
        })

        disposables.add(currencyManager.subject.subscribe {
            updateRates()
        })

        disposables.add(networkAvailabilityManager.stateSubject.subscribe { connected ->
            if (connected) {
                updateRates()
            }
        })
    }

    fun rate(coin: String, currencyCode: String): Maybe<Rate> {
        return storage.rate(coin, currencyCode)
    }

    private fun updateRates() {
        val coins = walletManager.wallets.map { it.coin }
        val currencyCode = currencyManager.baseCurrency.code

        syncer.sync(coins = coins, currencyCode = currencyCode)
    }

    override fun didSync(coin: String, currencyCode: String, latestRate: LatestRate) {
        storage.save(latestRate = latestRate, coin = coin, currencyCode = currencyCode)
        subject.onNext(true)
    }

    override fun onFire() {
        updateRates()
    }
}
