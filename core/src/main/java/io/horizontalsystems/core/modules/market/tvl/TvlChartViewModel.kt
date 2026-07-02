package io.horizontalsystems.core.modules.market.tvl

import io.horizontalsystems.core.modules.chart.ChartCurrencyValueFormatterShortened
import io.horizontalsystems.core.modules.chart.ChartViewModel

class TvlChartViewModel(
    private val tvlChartService: TvlChartService,
    chartCurrencyValueFormatter: ChartCurrencyValueFormatterShortened,
) : ChartViewModel(tvlChartService, chartCurrencyValueFormatter) {

    fun onSelectChain(chain: TvlModule.Chain) {
        tvlChartService.chain = chain
    }

}
