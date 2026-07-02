package io.horizontalsystems.core.modules.market.tvl

import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.core.core.managers.CurrencyManager
import io.horizontalsystems.core.core.stats.StatEvent
import io.horizontalsystems.core.core.stats.StatPage
import io.horizontalsystems.core.core.stats.stat
import io.horizontalsystems.core.core.stats.statPeriod
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.modules.chart.AbstractChartService
import io.horizontalsystems.core.modules.chart.ChartPointsWrapper
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single

class TvlChartService(
    override val currencyManager: CurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Day1

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

    var chain: TvlModule.Chain = TvlModule.Chain.All
        set(value) {
            field = value
            dataInvalidated()
        }

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency
    ): Single<ChartPointsWrapper> {
        val chainParam = if (chain == TvlModule.Chain.All) "" else chain.name
        return globalMarketRepository.getTvlGlobalMarketPoints(
            chainParam,
            currency.code,
            chartInterval
        ).map {
            ChartPointsWrapper(it)
        }
    }

    override fun updateChartInterval(chartInterval: HsTimePeriod?) {
        super.updateChartInterval(chartInterval)

        stat(
            page = StatPage.GlobalMetricsTvlInDefi,
            event = StatEvent.SwitchChartPeriod(chartInterval.statPeriod)
        )
    }
}
