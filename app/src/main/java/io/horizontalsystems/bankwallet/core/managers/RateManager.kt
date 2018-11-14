package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IPeriodicTimerDelegate
import io.horizontalsystems.bankwallet.core.IRateSyncerDelegate
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Rate
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class RateManager(
//        private val storage: RateStorage,
        private val syncer: RateSyncer,
        walletManager: IWalletManager,
        private val currencyManager: ICurrencyManager,
        networkAvailabilityManager: NetworkAvailabilityManager,
        timer: PeriodicTimer) : IPeriodicTimerDelegate, IRateSyncerDelegate {

    var rateList = listOf<Rate>()
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

    fun rate(coin: String, currencyCode: String): Rate? {
        return Rate("BTC", "USD", 6300.0, 1542080725000)
        //return storage.rate(forCoin = coin, currencyCode = currencyCode)
    }

    private fun updateRates() {
        val coins = listOf("BTC","BCH","ETH") //walletManager.wallets.map { $0.coin }
        val currencyCode = currencyManager.baseCurrency.code

        syncer.sync(coins = coins, currencyCode = currencyCode)
    }

    override fun didSync(coin: String, currencyCode: String, value: Double) {
//        storage.save(value: value, coin: coin, currencyCode: currencyCode)
        subject.onNext(true)
    }

    override fun onFire() {
        updateRates()
    }
}
