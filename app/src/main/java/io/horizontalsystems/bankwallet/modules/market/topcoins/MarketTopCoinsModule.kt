package io.horizontalsystems.bankwallet.modules.market.topcoins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.overview.TopMarketsRepository
import io.horizontalsystems.bankwallet.ui.compose.Select

object MarketTopCoinsModule {

    class Factory(
        private val topMarket: TopMarket? = null,
        private val sortingField: SortingField? = null,
        private val marketField: MarketField? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val topMarketsRepository = TopMarketsRepository(App.marketKit)
            val service = MarketTopCoinsService(
                topMarketsRepository,
                App.currencyManager,
                topMarket ?: defaultTopMarket,
                sortingField ?: defaultSortingField
            )
            return MarketTopCoinsViewModel(service, marketField ?: defaultMarketField) as T
        }

        companion object {
            val defaultSortingField = SortingField.HighestCap
            val defaultTopMarket = TopMarket.Top250
            val defaultMarketField = MarketField.PriceDiff
        }
    }

    data class Menu(
        val sortingFieldSelect: Select<SortingField>,
        val topMarketSelect: Select<TopMarket>?,
        val marketFieldSelect: Select<MarketField>
    )

}

sealed class SelectorDialogState() {
    object Closed : SelectorDialogState()
    class Opened(val select: Select<SortingField>) : SelectorDialogState()
}
