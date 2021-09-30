package io.horizontalsystems.bankwallet.modules.market.marketglobal

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.reactivex.Single

class MarketGlobalFetcher(
        private val rateManager: IRateManager,
        private val metricsType: MetricsType
) : MetricChartModule.IMetricChartFetcher, MetricChartModule.IMetricChartConfiguration {

    override val title = metricsType.title

    override val description = metricsType.description

    override val valueType: MetricChartModule.ValueType
        get() = when (metricsType) {
            MetricsType.BtcDominance -> MetricChartModule.ValueType.Percent
            else -> MetricChartModule.ValueType.CompactCurrencyValue
        }

    override fun fetchSingle(currencyCode: String, timePeriod: TimePeriod): Single<List<MetricChartModule.Item>> {
        return rateManager.getGlobalCoinMarketPointsAsync(currencyCode, timePeriod)
                .map { list ->
                    list.map { point ->
                        val value = when (metricsType) {
                            MetricsType.TotalMarketCap -> point.marketCap
                            MetricsType.BtcDominance -> point.btcDominance
                            MetricsType.Volume24h -> point.volume24h
                            MetricsType.DefiCap -> point.defiMarketCap
                            MetricsType.TvlInDefi -> point.defiTvl
                        }
                        MetricChartModule.Item(value, point.timestamp)
                    }
                }
    }
}
