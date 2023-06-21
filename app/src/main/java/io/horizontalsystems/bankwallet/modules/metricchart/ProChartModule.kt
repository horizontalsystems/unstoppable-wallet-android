package io.horizontalsystems.bankwallet.modules.metricchart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.chart.ChartCurrencyValueFormatterShortened
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
import io.horizontalsystems.bankwallet.modules.chart.ChartNumberFormatterShortened

object ProChartModule {

    class Factory(
        private val coinUid: String,
        private val chartType: ChartType,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val chartService = ProChartService(
                App.currencyManager,
                App.marketKit,
                coinUid,
                chartType
            )
            val chartNumberFormatter = when (chartType) {
                ChartType.CexVolume,
                ChartType.DexVolume,
                ChartType.Tvl,
                ChartType.DexLiquidity -> ChartCurrencyValueFormatterShortened()
                ChartType.TxCount,
                ChartType.AddressesCount -> ChartNumberFormatterShortened()
            }
            return ChartModule.createViewModel(chartService, chartNumberFormatter) as T
        }
    }

    enum class ChartType(val titleRes: Int) {
        CexVolume(R.string.CoinAnalytics_CexVolume),
        DexVolume(R.string.CoinAnalytics_DexVolume),
        DexLiquidity(R.string.CoinAnalytics_DexLiquidity),
        TxCount(R.string.CoinAnalytics_TransactionCount),
        AddressesCount(R.string.CoinAnalytics_ActiveAddresses),
        Tvl(R.string.CoinAnalytics_ProjectTvl)
    }

}
