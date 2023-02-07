package cash.p.terminal.modules.market.platform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.chart.ChartCurrencyValueFormatterShortened
import cash.p.terminal.modules.chart.ChartModule
import cash.p.terminal.modules.chart.ChartViewModel
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.topplatforms.Platform
import cash.p.terminal.ui.compose.Select

object MarketPlatformModule {

    class Factory(private val platform: Platform) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                MarketPlatformViewModel::class.java -> {
                    val repository =
                        MarketPlatformCoinsRepository(platform, App.marketKit, App.currencyManager)
                    MarketPlatformViewModel(platform, repository, App.marketFavoritesManager) as T
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
