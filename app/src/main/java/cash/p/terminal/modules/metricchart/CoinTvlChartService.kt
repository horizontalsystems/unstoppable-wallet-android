package cash.p.terminal.modules.metricchart

import cash.p.terminal.core.managers.DefaultCurrencyManager
import cash.p.terminal.wallet.MarketKitWrapper
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.chartview.chart.AbstractChartService
import io.horizontalsystems.chartview.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.models.HsTimePeriod
import kotlinx.coroutines.rx2.await

class CoinTvlChartService(
    override val currencyManager: DefaultCurrencyManager,
    private val marketKit: MarketKitWrapper,
    private val coinUid: String,
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Month1
    override val chartIntervals = HsTimePeriod.values().toList()
    override val chartViewType = ChartViewType.Line

    override suspend fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency
    ): ChartPointsWrapper =
        marketKit.marketInfoTvlSingle(coinUid, currency.code, chartInterval)
            .map { info ->
                info.map { ChartPoint(it.value.toFloat(), it.timestamp) }
            }
            .let { ChartPointsWrapper(it.await()) }
}
