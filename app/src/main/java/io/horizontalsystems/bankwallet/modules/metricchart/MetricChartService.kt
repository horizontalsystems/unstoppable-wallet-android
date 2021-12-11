package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.ChartType
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MetricChartService(
    val currency: Currency,
    private val fetcher: IMetricChartFetcher,
) {

    val stateObservable: BehaviorSubject<DataState<Pair<ChartType, List<MetricChartModule.Item>>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    private var chartInfoDisposable: Disposable? = null

    val chartTypes by fetcher::chartTypes
    val title by fetcher::title
    val description by fetcher::description
    val poweredBy by fetcher::poweredBy

    fun start() {
        updateChartType(fetcher.initialChartType)
    }

    fun stop() {
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

}
