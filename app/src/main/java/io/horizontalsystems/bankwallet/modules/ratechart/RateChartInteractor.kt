package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IRateStatsManager
import io.horizontalsystems.bankwallet.core.IRateStatsSyncer
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.core.managers.StatsData
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class RateChartInteractor(
        private val rateStatsManager: IRateStatsManager,
        private val rateStatsSyncer: IRateStatsSyncer,
        private val rateStorage: IRateStorage,
        private val localStorage: ILocalStorage)
    : RateChartModule.Interactor {

    private val disposables = CompositeDisposable()
    var delegate: RateChartModule.InteractorDelegate? = null

    override var chartEnabled: Boolean
        get() = rateStatsSyncer.rateChartShown
        set(value) {
            rateStatsSyncer.rateChartShown = value
        }

    override var defaultChartType: ChartType
        get() = localStorage.chartMode
        set(value) {
            localStorage.chartMode = value
        }

    override fun subscribeToChartStats() {
        rateStatsManager.statsFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it is StatsData) {
                        delegate?.onReceiveStats(it)
                    }
                }, {
                    delegate?.onReceiveError(it)
                })
                .let { disposables.add(it) }
    }

    override fun subscribeToLatestRate(coinCode: String, currencyCode: String) {
        rateStorage.latestRateObservable(coinCode, currencyCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    delegate?.onReceiveLatestRate(it)
                }, {
                    delegate?.onReceiveError(it)
                })
                .let { disposables.add(it) }
    }

    override fun clear() {
        disposables.clear()
    }

}
