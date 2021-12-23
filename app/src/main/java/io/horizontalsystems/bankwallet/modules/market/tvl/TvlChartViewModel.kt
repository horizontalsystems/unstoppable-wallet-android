package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFactory

class TvlChartViewModel(
    private val tvlChartService: TvlChartRepo,
    factory: MetricChartFactory,
) : ChartViewModel(tvlChartService, factory) {

    fun onSelectChain(chain: TvlModule.Chain) {
        tvlChartService.chain = chain
    }

}
