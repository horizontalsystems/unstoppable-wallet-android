package cash.p.terminal.modules.market.topcoins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.MarketViewItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.modules.market.TopMarket
import cash.p.terminal.ui.compose.Select

object MarketTopCoinsModule {

    class Factory(
        private val topMarket: TopMarket? = null,
        private val sortingField: SortingField? = null,
        private val marketField: MarketField? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val topMarketsRepository = MarketTopMoversRepository(App.marketKit)
            val service = MarketTopCoinsService(
                topMarketsRepository,
                App.currencyManager,
                App.marketFavoritesManager,
                topMarket ?: defaultTopMarket,
                sortingField ?: defaultSortingField,
            )
            return MarketTopCoinsViewModel(
                service,
                marketField ?: defaultMarketField
            ) as T
        }

        companion object {
            val defaultSortingField = SortingField.HighestCap
            val defaultTopMarket = TopMarket.Top100
            val defaultMarketField = MarketField.PriceDiff
        }
    }

    data class UiState(
        val viewItems: List<MarketViewItem>,
        val viewState: ViewState,
        val isRefreshing: Boolean,
        val sortingField: SortingField,
        val topMarket: TopMarket,
        val timeDuration: TimeDuration,
    )

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
