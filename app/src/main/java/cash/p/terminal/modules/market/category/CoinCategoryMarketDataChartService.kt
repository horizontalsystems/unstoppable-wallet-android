package cash.p.terminal.modules.market.category

import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.chartview.chart.AbstractChartService
import io.horizontalsystems.chartview.chart.ChartPointsWrapper
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.models.HsTimePeriod
import kotlinx.coroutines.rx2.await

class CoinCategoryMarketDataChartService(
    override val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
    private val categoryUid: String,
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Day1
    override val chartIntervals = listOf(HsTimePeriod.Day1, HsTimePeriod.Week1, HsTimePeriod.Month1)
    override val chartViewType = ChartViewType.Line

    override suspend fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency
    ): ChartPointsWrapper = marketKit.coinCategoryMarketPointsSingle(categoryUid, chartInterval, currency.code)
        .map { info ->
            info.map { ChartPoint(it.marketCap.toFloat(), it.timestamp) }
        }
        .map { ChartPointsWrapper(it) }.await()

    override fun updateChartInterval(chartInterval: HsTimePeriod?) {
        super.updateChartInterval(chartInterval)
    }
}
