package cash.p.terminal.modules.market.metricspage

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.entities.Currency
import cash.p.terminal.modules.chart.AbstractChartService
import cash.p.terminal.modules.chart.ChartPointsWrapper
import cash.p.terminal.modules.market.tvl.GlobalMarketRepository
import cash.p.terminal.modules.metricchart.MetricsType
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single

class MetricsPageChartService(
    override val currencyManager: CurrencyManager,
    private val metricsType: MetricsType,
    private val globalMarketRepository: GlobalMarketRepository,
) : AbstractChartService() {

    override val initialChartInterval: HsTimePeriod = HsTimePeriod.Day1

    override val chartIntervals = HsTimePeriod.values().toList()

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency,
    ): Single<ChartPointsWrapper> {
        return globalMarketRepository.getGlobalMarketPoints(
            currency.code,
            chartInterval,
            metricsType
        ).map {
            ChartPointsWrapper(chartInterval, it)
        }
    }
}
