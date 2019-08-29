package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.core.managers.RateStatsManager
import io.horizontalsystems.bankwallet.core.managers.RateStatsManager.StatsKey
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.RateStatData
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers

class RateChartInteractor(
        private val rateStatsManager: RateStatsManager,
        private val rateStorage: IRateStorage,
        private val localStorage: ILocalStorage)
    : RateChartModule.Interactor {

    var delegate: RateChartModule.InteractorDelegate? = null

    private val disposable = CompositeDisposable()

    override var defaultChartType: ChartType
        get() = localStorage.chartMode
        set(value) {
            localStorage.chartMode = value
        }

    override fun getRateStats(coinCode: CoinCode, currencyCode: String) {
        val getRates = rateStatsManager.getRateStats(coinCode, currencyCode)
        val getLocalRate = rateStorage.latestRateObservable(coinCode, currencyCode)

        val zipper = BiFunction<Pair<StatsKey, RateStatData>, Rate, Pair<Pair<StatsKey, RateStatData>, Rate>> { a, b ->
            Pair(a, b)
        }

        Flowable.zip(getRates, getLocalRate, zipper)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ (data, rate) ->
                    delegate?.onReceiveStats(Pair(data.second, rate))
                }, {
                    delegate?.onReceiveError(it)
                })
                .let { disposable.add(it) }
    }

    override fun clear() {
        disposable.clear()
    }
}
