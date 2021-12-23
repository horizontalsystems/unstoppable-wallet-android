package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartDataXxx
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Single

class TvlChartService(
    override val currencyManager: ICurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository
): AbstractChartService() {

    override val initialChartType: ChartView.ChartType = ChartView.ChartType.DAILY

    override val chartTypes = listOf(ChartView.ChartType.DAILY,
        ChartView.ChartType.WEEKLY,
        ChartView.ChartType.MONTHLY)

    var chain: TvlModule.Chain = TvlModule.Chain.All
        set(value) {
            field = value
            dataInvalidated()
        }

    override fun getItems(chartType: ChartView.ChartType, currency: Currency): Single<ChartDataXxx> {
        val chainParam = if (chain == TvlModule.Chain.All) "" else chain.name
        return globalMarketRepository.getTvlGlobalMarketPoints(chainParam, currency.code, chartType)
            .map {
                ChartDataXxx(chartType, it)
            }
    }
}
