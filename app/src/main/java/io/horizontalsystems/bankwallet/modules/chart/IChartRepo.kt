package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import io.reactivex.Single

interface IChartRepo {
    val chartTypes: List<ChartView.ChartType>
    val dataUpdatedObservable: Observable<Unit>

    fun getItems(chartType: ChartView.ChartType, currency: Currency) : Single<List<MetricChartModule.Item>>
}