package cash.p.terminal.modules.market.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.chart.ChartCurrencyValueFormatterShortened
import io.horizontalsystems.chartview.chart.ChartModule
import io.horizontalsystems.chartview.chart.ChartViewModel
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.MarketItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TopMarket
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.wallet.models.Analytics
import cash.p.terminal.wallet.models.CoinCategory

object MarketCategoryModule {

    class Factory(
        private val coinCategory: CoinCategory
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                MarketCategoryViewModel::class.java -> {
                    val marketCategoryRepository = MarketCategoryRepository(App.marketKit)
                    val service = MarketCategoryService(
                        marketCategoryRepository,
                        App.currencyManager,
                        App.languageManager,
                        App.marketFavoritesManager,
                        coinCategory,
                        defaultTopMarket,
                        defaultSortingField
                    )
                    MarketCategoryViewModel(service) as T
                }

                ChartViewModel::class.java -> {
                    val chartService = CoinCategoryMarketDataChartService(
                        App.currencyManager,
                        App.marketKit,
                        coinCategory.uid
                    )
                    val chartNumberFormatter = ChartCurrencyValueFormatterShortened()
                    ChartModule.createViewModel(chartService, chartNumberFormatter) as T
                }
                else -> throw IllegalArgumentException()
            }
        }

        companion object {
            val defaultSortingField = SortingField.HighestCap
            val defaultTopMarket = TopMarket.Top100
        }
    }

    data class Menu(
        val sortingFieldSelect: Select<SortingField>,
        val marketFieldSelect: Select<MarketField>
    )

}

data class MarketItemWrapper(
    val marketItem: MarketItem,
    val favorited: Boolean,
    val signal: Analytics.TechnicalAdvice.Advice? =  null
)