package io.horizontalsystems.bankwallet.modules.market.marketglobal

import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.TimePeriod
import io.reactivex.Single

class MarketGlobalFetcher(
    private val marketKit: MarketKit,
    private val metricsType: MetricsType
) : MetricChartModule.IMetricChartConfiguration {

    override val title = metricsType.title

    override val description = metricsType.description

    override val valueType: MetricChartModule.ValueType
        get() = when (metricsType) {
            MetricsType.BtcDominance -> MetricChartModule.ValueType.Percent
            else -> MetricChartModule.ValueType.CompactCurrencyValue
        }

    fun fetchSingle(currencyCode: String, timePeriod: TimePeriod): Single<List<MetricChartModule.Item>> {
        return marketKit.globalMarketPointsSingle(currencyCode, timePeriod)
            .map { list ->
                list.map { point ->
                    val value = when (metricsType) {
                        MetricsType.TotalMarketCap -> point.marketCap
                        MetricsType.BtcDominance -> point.dominanceBtc
                        MetricsType.Volume24h -> point.volume24h
                        MetricsType.DefiCap -> point.marketCapDefi
                        MetricsType.TvlInDefi -> point.tvl
                    }
                    MetricChartModule.Item(value, null, point.timestamp)
                }
            }
    }
}
