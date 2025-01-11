package cash.p.terminal.modules.market.tvl

import cash.p.terminal.modules.chart.ChartCurrencyValueFormatterShortened
import io.horizontalsystems.chartview.chart.ChartViewModel

class TvlChartViewModel(
    private val tvlChartService: TvlChartService,
    chartCurrencyValueFormatter: ChartCurrencyValueFormatterShortened,
) : ChartViewModel(tvlChartService, chartCurrencyValueFormatter) {

    fun onSelectChain(chain: TvlModule.Chain) {
        tvlChartService.chain = chain
    }

}
