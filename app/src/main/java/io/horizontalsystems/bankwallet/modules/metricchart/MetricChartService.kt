package io.horizontalsystems.bankwallet.modules.metricchart

class MetricChartService(
    private val fetcher: IMetricChartFetcher,
) {
    val title by fetcher::title
    val description by fetcher::description
    val poweredBy by fetcher::poweredBy
}
