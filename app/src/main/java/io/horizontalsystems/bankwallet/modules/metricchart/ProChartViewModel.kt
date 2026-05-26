package io.horizontalsystems.bankwallet.modules.metricchart

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.modules.chart.ChartCurrencyValueFormatterShortened
import io.horizontalsystems.bankwallet.modules.chart.ChartNumberFormatterShortened
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel

@HiltViewModel(assistedFactory = ProChartViewModel.Factory::class)
class ProChartViewModel @AssistedInject constructor(
    @Assisted coinUid: String,
    @Assisted chartType: ProChartModule.ChartType,
    currencyManager: CurrencyManager,
    marketKit: MarketKitWrapper,
) : ChartViewModel(
    service = ProChartService(currencyManager, marketKit, coinUid, chartType),
    valueFormatter = when (chartType) {
        ProChartModule.ChartType.CexVolume,
        ProChartModule.ChartType.DexVolume,
        ProChartModule.ChartType.Tvl,
        ProChartModule.ChartType.DexLiquidity -> ChartCurrencyValueFormatterShortened()
        ProChartModule.ChartType.TxCount,
        ProChartModule.ChartType.AddressesCount -> ChartNumberFormatterShortened()
    }
) {
    @AssistedFactory
    interface Factory {
        fun create(coinUid: String, chartType: ProChartModule.ChartType): ProChartViewModel
    }
}
