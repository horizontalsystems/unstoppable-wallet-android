package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.core.UnsupportedException
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.ChartView.ChartType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single

class CoinTvlChartService(
    override val currencyManager: ICurrencyManager,
    private val marketKit: MarketKit,
    private val coinUid: String,
) : AbstractChartService() {

    override val initialChartType = ChartType.MONTHLY
    override val chartTypes = listOf(
        ChartType.DAILY,
        ChartType.WEEKLY,
        ChartType.MONTHLY,
    )

    override fun getItems(chartType: ChartType, currency: Currency): Single<ChartPointsWrapper> = try {
        val timePeriod = getTimePeriod(chartType)
        marketKit.marketInfoTvlSingle(coinUid, currency.code, timePeriod)
            .map { info ->
                info.map { point ->
                    ChartPoint(point.value.toFloat(), point.timestamp)
                }
            }
            .map {
                ChartPointsWrapper(chartType, it)
            }
    } catch (e: Exception) {
        Single.error(e)
    }

    private fun getTimePeriod(chartType: ChartType) = when (chartType) {
        ChartType.DAILY -> HsTimePeriod.Day1
        ChartType.WEEKLY -> HsTimePeriod.Week1
        ChartType.MONTHLY -> HsTimePeriod.Month1
        else -> throw UnsupportedException("Unsupported chartType $chartType")
    }
}
