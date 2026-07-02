package io.horizontalsystems.walletkit.modules.market.tvl

import io.horizontalsystems.walletkit.modules.chart.ChartCurrencyValueFormatterShortened
import io.horizontalsystems.walletkit.modules.chart.ChartViewModel

class TvlChartViewModel(
    private val tvlChartService: TvlChartService,
    chartCurrencyValueFormatter: ChartCurrencyValueFormatterShortened,
) : ChartViewModel(tvlChartService, chartCurrencyValueFormatter) {

    fun onSelectChain(chain: TvlModule.Chain) {
        tvlChartService.chain = chain
    }

}
