package com.quantum.wallet.bankwallet.modules.market.platform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.modules.chart.ChartCurrencyValueFormatterShortened
import com.quantum.wallet.bankwallet.modules.chart.ChartModule
import com.quantum.wallet.bankwallet.modules.chart.ChartViewModel
import com.quantum.wallet.bankwallet.modules.market.MarketField
import com.quantum.wallet.bankwallet.modules.market.SortingField
import com.quantum.wallet.bankwallet.modules.market.topplatforms.Platform
import com.quantum.wallet.bankwallet.ui.compose.Select

object MarketPlatformModule {

    class Factory(private val platform: Platform) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                MarketPlatformViewModel::class.java -> {
                    val repository =
                        MarketPlatformCoinsRepository(platform, App.marketKit, App.currencyManager)
                    MarketPlatformViewModel(repository, App.marketFavoritesManager) as T
                }

                ChartViewModel::class.java -> {
                    val chartService =
                        PlatformChartService(platform, App.currencyManager, App.marketKit)
                    val chartNumberFormatter = ChartCurrencyValueFormatterShortened()
                    ChartModule.createViewModel(chartService, chartNumberFormatter) as T
                }
                else -> throw IllegalArgumentException()
            }
        }

    }

    data class Menu(
        val sortingFieldSelect: Select<SortingField>,
        val marketFieldSelect: Select<MarketField>
    )

}
