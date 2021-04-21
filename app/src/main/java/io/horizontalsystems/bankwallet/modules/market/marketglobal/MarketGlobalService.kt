package io.horizontalsystems.bankwallet.modules.market.marketglobal

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.GlobalCoinMarketPoint
import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MarketGlobalService(
        val currency: Currency,
        private val xRateManager: IRateManager,
): Clearable {

    val chartPointsUpdatedObservable: BehaviorSubject<Unit> = BehaviorSubject.create()
    val chartInfoErrorObservable: BehaviorSubject<Throwable> = BehaviorSubject.create()

    private val disposables = CompositeDisposable()

    private var chartInfoDisposable: Disposable? = null

    var chartPoints: List<GlobalCoinMarketPoint>? = null
        set(value) {
            field = value
            chartPointsUpdatedObservable.onNext(Unit)
        }

    override fun clear() {
       chartInfoDisposable?.dispose()
    }

    fun updateChartInfo(chartType: ChartView.ChartType) {
        chartInfoDisposable?.dispose()

        val timePeriod = getTimePeriod(chartType)

        xRateManager.getGlobalCoinMarketPointsAsync(currency.code, timePeriod)
                .subscribeIO({
                    chartPoints = it
                }, {
                    chartInfoErrorObservable.onNext(it)
                }).let {
                    chartInfoDisposable = it
                }
    }

    private fun getTimePeriod(chartType: ChartView.ChartType): TimePeriod{
        return when(chartType){
            ChartView.ChartType.DAILY -> TimePeriod.HOUR_24
            ChartView.ChartType.WEEKLY -> TimePeriod.DAY_7
            ChartView.ChartType.MONTHLY -> TimePeriod.DAY_30
            else -> throw IllegalArgumentException("Wrong ChartType")
        }
    }
}
