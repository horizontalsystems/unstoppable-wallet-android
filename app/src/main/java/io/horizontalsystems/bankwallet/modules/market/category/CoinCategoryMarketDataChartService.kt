package io.horizontalsystems.bankwallet.modules.market.category

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single

class CoinCategoryMarketDataChartService(
    override val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
    private val categoryUid: String,
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Day1
    override val chartIntervals = listOf(HsTimePeriod.Day1, HsTimePeriod.Week1, HsTimePeriod.Month1)
    override val chartViewType = ChartViewType.Line

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency
    ): Single<ChartPointsWrapper> = try {
        marketKit.coinCategoryMarketPointsSingle(categoryUid, chartInterval, currency.code)
            .map { info ->
                info.map { ChartPoint(it.marketCap.toFloat(), it.timestamp) }
            }
            .map { ChartPointsWrapper(it) }
    } catch (e: Exception) {
        Single.error(e)
    }

}
