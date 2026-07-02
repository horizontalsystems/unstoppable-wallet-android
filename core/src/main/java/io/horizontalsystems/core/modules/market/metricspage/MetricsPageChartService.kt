package io.horizontalsystems.core.modules.market.metricspage

import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.core.core.managers.CurrencyManager
import io.horizontalsystems.core.core.stats.StatEvent
import io.horizontalsystems.core.core.stats.stat
import io.horizontalsystems.core.core.stats.statPage
import io.horizontalsystems.core.core.stats.statPeriod
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.modules.chart.AbstractChartService
import io.horizontalsystems.core.modules.chart.ChartPointsWrapper
import io.horizontalsystems.core.modules.market.tvl.GlobalMarketRepository
import io.horizontalsystems.core.modules.metricchart.MetricsType
import io.horizontalsystems.marketkit.models.HsTimePeriod
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
        HsTimePeriod.Week2,
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

        stat(
            page = metricsType.statPage,
            event = StatEvent.SwitchChartPeriod(chartInterval.statPeriod)
        )
    }
}
