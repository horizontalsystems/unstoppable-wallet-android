package io.horizontalsystems.walletkit.modules.market.metricspage

import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.core.stats.statPage
import io.horizontalsystems.walletkit.core.stats.statPeriod
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.modules.chart.AbstractChartService
import io.horizontalsystems.walletkit.modules.chart.ChartPointsWrapper
import io.horizontalsystems.walletkit.modules.market.tvl.GlobalMarketRepository
import io.horizontalsystems.walletkit.modules.metricchart.MetricsType
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
