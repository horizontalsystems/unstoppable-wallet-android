package cash.p.terminal.modules.metricchart

import cash.p.terminal.core.managers.DefaultCurrencyManager
import cash.p.terminal.wallet.MarketKitWrapper
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.chartview.chart.AbstractChartService
import io.horizontalsystems.chartview.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.models.HsTimePeriod
import io.reactivex.Single

class CoinTvlChartService(
    override val currencyManager: DefaultCurrencyManager,
    private val marketKit: MarketKitWrapper,
    private val coinUid: String,
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Month1
    override val chartIntervals = HsTimePeriod.values().toList()
    override val chartViewType = ChartViewType.Line

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency
    ): Single<ChartPointsWrapper> = try {
        marketKit.marketInfoTvlSingle(coinUid, currency.code, chartInterval)
            .map { info ->
                info.map { ChartPoint(it.value.toFloat(), it.timestamp) }
            }
            .map { ChartPointsWrapper(it) }
    } catch (e: Exception) {
        Single.error(e)
    }

}
