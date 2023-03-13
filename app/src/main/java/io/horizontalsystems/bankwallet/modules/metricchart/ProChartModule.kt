package io.horizontalsystems.bankwallet.modules.metricchart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.chart.ChartCoinValueFormatterShortened
import io.horizontalsystems.bankwallet.modules.chart.ChartCurrencyValueFormatterShortened
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
import io.horizontalsystems.bankwallet.modules.chart.ChartNumberFormatterShortened

object ProChartModule {

    class Factory(private val coinUid: String, private val chartType: ChartType) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val fullCoin = App.marketKit.fullCoins(coinUids = listOf(coinUid)).first()

            val chartService = ProChartService(App.currencyManager, App.proFeatureAuthorizationManager, App.marketKit, coinUid, chartType)
            val chartNumberFormatter = when (chartType) {
                ChartType.DexVolume, ChartType.DexLiquidity -> ChartCurrencyValueFormatterShortened()
                ChartType.TxVolume -> ChartCoinValueFormatterShortened(fullCoin)
                ChartType.TxCount, ChartType.AddressesCount -> ChartNumberFormatterShortened()
            }
            return ChartModule.createViewModel(chartService, chartNumberFormatter) as T
        }
    }

    enum class ChartType(val titleRes: Int, val descriptionRes: Int) {
        DexVolume(R.string.CoinPage_DetailsDexVolume, R.string.CoinPage_DetailsDexVolume_Description),
        DexLiquidity(R.string.CoinPage_DetailsDexLiquidity, R.string.CoinPage_DetailsDexLiquidity_Description),
        TxCount(R.string.CoinPage_DetailsTxCount, R.string.CoinPage_DetailsTxCount_Description),
        TxVolume(R.string.CoinPage_DetailsTxVolume, R.string.CoinPage_DetailsTxVolume_Description),
        AddressesCount(R.string.CoinPage_DetailsActiveAddresses, R.string.CoinPage_DetailsActiveAddresses_Description)
    }

}
