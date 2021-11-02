package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.marketglobal.MarketGlobalFetcher
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.TimePeriod
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MetricChartService(
    val currency: Currency,
    private val fetcher: MarketGlobalFetcher,
) : Clearable {

    val stateObservable: BehaviorSubject<DataState<List<MetricChartModule.Item>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    private var chartInfoDisposable: Disposable? = null

    override fun clear() {
        chartInfoDisposable?.dispose()
    }

    fun updateChartInfo(chartType: ChartView.ChartType) {
        chartInfoDisposable?.dispose()

        stateObservable.onNext(DataState.Loading)

        val timePeriod = getTimePeriod(chartType)

        fetcher.fetchSingle(currency.code, timePeriod)
            .subscribeIO({
                stateObservable.onNext(DataState.Success(it))
            }, {
                stateObservable.onNext(DataState.Error(it))
            }).let {
                chartInfoDisposable = it
            }
    }

    private fun getTimePeriod(chartType: ChartView.ChartType): TimePeriod {
        return when (chartType) {
            ChartView.ChartType.DAILY -> TimePeriod.Hour24
            ChartView.ChartType.WEEKLY -> TimePeriod.Day7
            ChartView.ChartType.MONTHLY -> TimePeriod.Day30
            else -> throw IllegalArgumentException("Wrong ChartType")
        }
    }
}
