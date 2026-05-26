package io.horizontalsystems.bankwallet.modules.coin.overview

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.modules.chart.ChartCurrencyValueFormatterSignificant
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.marketkit.models.FullCoin

@HiltViewModel(assistedFactory = CoinOverviewChartViewModel.Factory::class)
class CoinOverviewChartViewModel @AssistedInject constructor(
    @Assisted fullCoin: FullCoin,
    marketKit: MarketKitWrapper,
    currencyManager: CurrencyManager,
    chartIndicatorManager: ChartIndicatorManager,
) : ChartViewModel(
    service = CoinOverviewChartService(marketKit, currencyManager, fullCoin.coin.uid, chartIndicatorManager),
    valueFormatter = ChartCurrencyValueFormatterSignificant()
) {
    @AssistedFactory
    interface Factory {
        fun create(fullCoin: FullCoin): CoinOverviewChartViewModel
    }
}
