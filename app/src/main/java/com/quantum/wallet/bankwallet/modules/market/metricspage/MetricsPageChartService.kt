package com.quantum.wallet.bankwallet.modules.market.metricspage

import com.quantum.wallet.bankwallet.core.managers.CurrencyManager
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.core.stats.statPage
import com.quantum.wallet.bankwallet.core.stats.statPeriod
import com.quantum.wallet.bankwallet.entities.Currency
import com.quantum.wallet.bankwallet.modules.chart.AbstractChartService
import com.quantum.wallet.bankwallet.modules.chart.ChartPointsWrapper
import com.quantum.wallet.bankwallet.modules.market.tvl.GlobalMarketRepository
import com.quantum.wallet.bankwallet.modules.metricchart.MetricsType
import com.quantum.wallet.chartview.ChartViewType
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
