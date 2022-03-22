package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.modules.chart.ChartNumberFormatterShortened
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel

class TvlChartViewModel(
    private val tvlChartService: TvlChartService,
    chartNumberFormatter: ChartNumberFormatterShortened,
) : ChartViewModel(tvlChartService, chartNumberFormatter) {

    fun onSelectChain(chain: TvlModule.Chain) {
        tvlChartService.chain = chain
    }

}
