package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartDataXxx
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.chartview.ChartView.ChartType
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.reactivex.Single
import io.horizontalsystems.marketkit.models.ChartType as KitChartType

class CoinTradingVolumeChartService(
    override val currencyManager: ICurrencyManager,
    private val marketKit: MarketKit,
    private val coinUid: String,
    private val coinName: String,
) : IMetricChartFetcher, AbstractChartService() {

    override val title = R.string.CoinPage_TotalVolume
    override val description = TranslatableString.ResString(R.string.MarketGlobalMetrics_VolumeDescriptionCoin, coinName)
    override val poweredBy = TranslatableString.ResString(R.string.Market_PoweredByApi)

    override val initialChartType = ChartType.MONTHLY_BY_DAY

    override val chartTypes = listOf(
        ChartType.MONTHLY_BY_DAY,
        ChartType.MONTHLY6,
        ChartType.MONTHLY12,
    )

    override fun getItems(
        chartType: ChartType,
        currency: Currency,
    ): Single<ChartDataXxx> {
        return marketKit.chartInfoSingle(coinUid, currency.code, chartType.kitChartType)
            .map { info ->
                val items = info.points
                    .filter { it.timestamp >= info.startTimestamp }
                    .mapNotNull { point ->
                        point.volume?.let {
                            MetricChartModule.Item(it, null, point.timestamp)
                        }
                    }
                ChartDataXxx(chartType, items, info.startTimestamp, info.endTimestamp, info.isExpired)
            }
    }
}

val ChartType.kitChartType: KitChartType
    get() = when (this) {
        ChartType.TODAY -> KitChartType.TODAY
        ChartType.DAILY -> KitChartType.DAILY
        ChartType.WEEKLY -> KitChartType.WEEKLY
        ChartType.WEEKLY2 -> KitChartType.WEEKLY2
        ChartType.MONTHLY -> KitChartType.MONTHLY
        ChartType.MONTHLY_BY_DAY -> KitChartType.MONTHLY_BY_DAY
        ChartType.MONTHLY3 -> KitChartType.MONTHLY3
        ChartType.MONTHLY6 -> KitChartType.MONTHLY6
        ChartType.MONTHLY12 -> KitChartType.MONTHLY12
        ChartType.MONTHLY24 -> KitChartType.MONTHLY24
    }
