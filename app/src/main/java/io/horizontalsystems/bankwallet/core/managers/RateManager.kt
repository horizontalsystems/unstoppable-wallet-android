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
        private val wordsManager: IWordsManager,
        networkAvailabilityManager: NetworkAvailabilityManager,
        timer: PeriodicTimer) : IPeriodicTimerDelegate, IRateSyncerDelegate {

    val subject: PublishSubject<Boolean> = PublishSubject.create()
    val latestRates = mutableMapOf<String, MutableMap<String, LatestRate>>()

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

        disposables.add(storage.getAll().subscribe {
            subject.onNext(true)
        })

        disposables.add(wordsManager.loggedInSubject
                .subscribe { logInState ->
                    if (logInState == LogInState.LOGOUT) {
                        storage.deleteAll()
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
        if (latestRates[coin] == null) {
            latestRates[coin] = mutableMapOf()
        }
        latestRates[coin]?.set(currencyCode, latestRate)

        storage.save(latestRate = latestRate, coin = coin, currencyCode = currencyCode)
    }

    override fun onFire() {
        updateRates()
    }
}
