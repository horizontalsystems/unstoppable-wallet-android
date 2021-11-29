package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.ChartType
import io.horizontalsystems.marketkit.models.TimePeriod
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MetricChartService(
    val currency: Currency,
    private val fetcher: IMetricChartFetcher,
) : Clearable {

    val stateObservable: BehaviorSubject<DataState<Pair<ChartType, List<MetricChartModule.Item>>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    private var chartInfoDisposable: Disposable? = null

    val chartTypes by fetcher::chartTypes

    override fun clear() {
        chartInfoDisposable?.dispose()
    }

    fun updateChartType(chartType: ChartType) {
        chartInfoDisposable?.dispose()

        stateObservable.onNext(DataState.Loading)

        fetcher.fetchSingle(currency.code, chartType)
            .subscribeIO({
                stateObservable.onNext(DataState.Success(Pair(chartType, it)))
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
