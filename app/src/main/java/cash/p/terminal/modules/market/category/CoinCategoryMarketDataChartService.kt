package cash.p.terminal.modules.market.category

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.entities.Currency
import cash.p.terminal.modules.chart.AbstractChartService
import cash.p.terminal.modules.chart.ChartPointsWrapper
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

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency
    ): Single<ChartPointsWrapper> = try {
        marketKit.coinCategoryMarketPointsSingle(categoryUid, chartInterval, currency.code)
            .map { info ->
                info.map { ChartPoint(it.marketCap.toFloat(), it.timestamp) }
            }
            .map { ChartPointsWrapper(chartInterval, it) }
    } catch (e: Exception) {
        Single.error(e)
    }

}
