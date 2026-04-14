package com.quantum.wallet.bankwallet.modules.market.tvl

import com.quantum.wallet.bankwallet.modules.chart.ChartCurrencyValueFormatterShortened
import com.quantum.wallet.bankwallet.modules.chart.ChartViewModel

class TvlChartViewModel(
    private val tvlChartService: TvlChartService,
    chartCurrencyValueFormatter: ChartCurrencyValueFormatterShortened,
) : ChartViewModel(tvlChartService, chartCurrencyValueFormatter) {

    fun onSelectChain(chain: TvlModule.Chain) {
        tvlChartService.chain = chain
    }

}
