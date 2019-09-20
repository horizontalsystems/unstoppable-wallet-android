package io.horizontalsystems.bankwallet.modules.ratelist

import android.app.Activity
import io.horizontalsystems.bankwallet.core.IRateStatsManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.core.managers.BackgroundManager
import io.horizontalsystems.bankwallet.core.managers.CurrentDateProvider
import io.horizontalsystems.bankwallet.core.managers.StatsData
import io.horizontalsystems.bankwallet.core.managers.StatsError
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class RatesInteractor(
        private val rateStatsManager: IRateStatsManager,
        private val rateStorage: IRateStorage,
        private val currentDateProvider: CurrentDateProvider,
        private val backgroundManager: BackgroundManager
) : RateListModule.IInteractor, BackgroundManager.Listener {

    init {
        backgroundManager.registerListener(this)
    }

    var delegate: RateListModule.IInteractorDelegate? = null
    private var disposables = CompositeDisposable()

    override fun willEnterForeground(activity: Activity) {
        delegate?.willEnterForeground()
    }

    override val currentDate: Date
        get() = currentDateProvider.currentDate

    override fun initRateList() {
        rateStatsManager.statsFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    when (it) {
                        is StatsData -> delegate?.onReceive(it)
                        is StatsError -> delegate?.onFailStats(it.coinCode)
                    }
                }, {
                })
                .let { disposables.add(it) }
    }

    override fun getRateStats(coinCodes: List<String>, currencyCode: String) {
        coinCodes.forEach { coinCode ->
            rateStatsManager.syncStats(coinCode, currencyCode)
        }
    }

    override fun fetchRates(coinCodes: List<String>, currencyCode: String) {
        coinCodes.forEach { coinCode ->
            rateStorage.latestRateObservable(coinCode, currencyCode)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe {
                        delegate?.didUpdateRate(it)
                    }
                    .let { disposables.add(it) }
        }
    }

    override fun clear() {
        backgroundManager.unregisterListener(this)
        disposables.clear()
    }
}
