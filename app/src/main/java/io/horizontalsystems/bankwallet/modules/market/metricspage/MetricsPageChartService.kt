package io.horizontalsystems.bankwallet.modules.market.metricspage

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartPointsWrapper
import io.horizontalsystems.bankwallet.modules.market.tvl.GlobalMarketRepository
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single

class MetricsPageChartService(
    override val currencyManager: CurrencyManager,
    private val metricsType: MetricsType,
    private val globalMarketRepository: GlobalMarketRepository,
) : AbstractChartService() {

    override val initialChartInterval: HsTimePeriod = HsTimePeriod.Day1

    override val chartIntervals = HsTimePeriod.values().toList()
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
}
