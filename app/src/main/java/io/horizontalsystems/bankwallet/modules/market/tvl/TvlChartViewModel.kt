package io.horizontalsystems.bankwallet.modules.market.tvl

import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.modules.chart.ChartCurrencyValueFormatterShortened
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import javax.inject.Inject

@HiltViewModel
class TvlChartViewModel @Inject constructor(
    marketKit: MarketKitWrapper,
    currencyManager: CurrencyManager,
) : ChartViewModel(TvlChartService(currencyManager, GlobalMarketRepository(marketKit)), ChartCurrencyValueFormatterShortened()) {
    private val tvlChartService get() = service as TvlChartService

    fun onSelectChain(chain: TvlModule.Chain) {
        tvlChartService.chain = chain
    }

}
