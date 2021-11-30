package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.marketkit.models.ChartType
import io.reactivex.Single

interface IMetricChartFetcher {
    val title: Int
    val chartTypes: List<ChartType>
    fun fetchSingle(currencyCode: String, chartType: ChartType): Single<List<MetricChartModule.Item>>
}