package io.horizontalsystems.core.modules.market.sector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.modules.chart.ChartCurrencyValueFormatterShortened
import io.horizontalsystems.core.modules.chart.ChartModule
import io.horizontalsystems.core.modules.chart.ChartViewModel
import io.horizontalsystems.core.modules.market.TopMarket
import io.horizontalsystems.marketkit.models.CoinCategory

object MarketSectorModule {

    class Factory(
        private val coinCategory: CoinCategory
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                MarketSectorViewModel::class.java -> {
                    val marketCategoryRepository = MarketSectorRepository(App.marketKit)
                    MarketSectorViewModel(
                        marketCategoryRepository,
                        App.currencyManager,
                        App.languageManager,
                        App.marketFavoritesManager,
                        coinCategory,
                        TopMarket.Top100,
                    ) as T
                }

                ChartViewModel::class.java -> {
                    val chartService = CoinSectorMarketDataChartService(
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
    }

}
