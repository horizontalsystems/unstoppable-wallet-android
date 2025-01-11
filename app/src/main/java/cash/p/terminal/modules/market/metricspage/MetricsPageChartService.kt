package cash.p.terminal.modules.market.metricspage

import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.stats.statPage
import cash.p.terminal.core.stats.statPeriod
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.chartview.chart.AbstractChartService
import io.horizontalsystems.chartview.chart.ChartPointsWrapper
import cash.p.terminal.modules.market.tvl.GlobalMarketRepository
import cash.p.terminal.modules.metricchart.MetricsType
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.core.models.HsTimePeriod
import io.reactivex.Single

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
        HsTimePeriod.Month3,
        HsTimePeriod.Month6,
        HsTimePeriod.Year1,
        HsTimePeriod.Year2,
    )

    override val chartViewType = ChartViewType.Line

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency,
    ): Single<ChartPointsWrapper> {
        return globalMarketRepository.getGlobalMarketPoints(
            currency.code,
            chartInterval,
            metricsType
        ).map {
            ChartPointsWrapper(it)
        }
    }

    override fun updateChartInterval(chartInterval: HsTimePeriod?) {
        super.updateChartInterval(chartInterval)

        stat(page = metricsType.statPage, event = StatEvent.SwitchChartPeriod(chartInterval.statPeriod))
    }
}
