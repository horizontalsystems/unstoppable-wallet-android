package io.horizontalsystems.bankwallet.modules.coin.metricchart

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.managers.TimePeriod
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.Single

class CoinTvlFetcher(
        private val rateManager: IRateManager,
        private val coinType: CoinType
):MetricChartModule.IMetricChartFetcher, MetricChartModule.IMetricChartConfiguration {

    override val title = R.string.CoinPage_Tvl

    override val description = R.string.CoinPage_TvlDescription

    override val valueType: MetricChartModule.ValueType
        get() = MetricChartModule.ValueType.CompactCurrencyValue

    override fun fetchSingle(currencyCode: String, timePeriod: TimePeriod): Single<List<MetricChartModule.Item>> {
        return rateManager.defiTvlPoints(coinType, currencyCode, timePeriod)
                .map { points ->
                    points.map { MetricChartModule.Item(it.tvl, it.timestamp) }
                }
    }
}
