package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel

class TvlChartViewModel(
    private val tvlChartService: TvlChartService,
) : ChartViewModel(tvlChartService) {

    fun onSelectChain(chain: TvlModule.Chain) {
        tvlChartService.chain = chain
    }

}
