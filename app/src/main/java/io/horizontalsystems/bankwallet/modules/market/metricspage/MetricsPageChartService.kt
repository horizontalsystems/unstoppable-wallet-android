package io.horizontalsystems.bankwallet.modules.market.metricspage

import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartDataXxx
import io.horizontalsystems.bankwallet.modules.market.tvl.GlobalMarketRepository
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Single

class MetricsPageChartService(
    override val currencyManager: ICurrencyManager,
    private val metricsType: MetricsType,
    private val globalMarketRepository: GlobalMarketRepository,
) : AbstractChartService() {

    override val initialChartType: ChartView.ChartType = ChartView.ChartType.DAILY

    override val chartTypes = listOf(ChartView.ChartType.DAILY,
        ChartView.ChartType.WEEKLY,
        ChartView.ChartType.MONTHLY)

    override fun getItems(
        chartType: ChartView.ChartType,
        currency: Currency,
    ): Single<ChartDataXxx> {
        return globalMarketRepository.getGlobalMarketPoints(currency.code, chartType, metricsType)
            .map {
                ChartDataXxx(chartType, it)
            }
    }
}
