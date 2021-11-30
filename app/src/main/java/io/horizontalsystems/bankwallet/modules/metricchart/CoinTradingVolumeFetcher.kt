package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.ChartType
import io.reactivex.Single

class CoinTradingVolumeFetcher(
    private val marketKit: MarketKit,
    private val coinUid: String,
) : IMetricChartFetcher {

    override val title: Int = R.string.CoinPage_TotalVolume

    override val chartTypes = listOf(
        ChartType.MONTHLY_BY_DAY,
        ChartType.MONTHLY6,
        ChartType.MONTHLY12,
    )

    override fun fetchSingle(
        currencyCode: String,
        chartType: ChartType,
    ): Single<List<MetricChartModule.Item>> {
        return marketKit.chartInfoSingle(coinUid, currencyCode, chartType)
            .map { info ->
                info.points.mapNotNull { point ->
                    point.volume?.let {
                        MetricChartModule.Item(it, null, point.timestamp)
                    }
                }
            }
    }
}
