package cash.p.terminal.modules.market.metricspage

import cash.p.terminal.core.stats.StatEvent
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.chartview.chart.AbstractChartService
import io.horizontalsystems.chartview.chart.ChartPointsWrapper
import cash.p.terminal.modules.market.tvl.GlobalMarketRepository
import cash.p.terminal.modules.metricchart.MetricsType
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.core.models.HsTimePeriod
import kotlinx.coroutines.rx2.await

class MetricsPageChartService(
    override val currencyManager: CurrencyManager,
    private val metricsType: MetricsType,
    private val globalMarketRepository: GlobalMarketRepository,
) : AbstractChartService() {

    override val initialChartInterval: HsTimePeriod = HsTimePeriod.Day1

    override val chartIntervals = listOf(
        HsTimePeriod.Day1,
        HsTimePeriod.Week1,
        HsTimePeriod.Month1,
        HsTimePeriod.Year1,
    )

    override val chartViewType = ChartViewType.Line

    override suspend fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency,
    ): ChartPointsWrapper = globalMarketRepository.getGlobalMarketPoints(
            currency.code,
            chartInterval,
            metricsType
        ).let {
            ChartPointsWrapper(it.await())
        }

    override fun updateChartInterval(chartInterval: HsTimePeriod?) {
        super.updateChartInterval(chartInterval)
    }
}
