package cash.p.terminal.modules.market.favorites

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.market.MarketViewItem
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.WithTranslatableTitle

object MarketFavoritesModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = MarketFavoritesRepository(App.marketKit, App.marketFavoritesManager)
            val menuService = MarketFavoritesMenuService(App.localStorage, App.marketWidgetManager)
            val service = MarketFavoritesService(
                repository,
                menuService,
                App.currencyManager,
                App.backgroundManager
            )
            return MarketFavoritesViewModel(service) as T
        }
    }

    data class UiState(
        val viewItems: List<MarketViewItem>,
        val viewState: ViewState,
        val isRefreshing: Boolean,
        val sortingField: WatchlistSorting,
        val period: TimeDuration,
        val showSignal: Boolean,
    )

}

enum class WatchlistSorting(@StringRes val titleResId: Int): WithTranslatableTitle {
    Manual(R.string.Market_Sorting_Manual),
    HighestCap(R.string.Market_Sorting_HighestCap),
    LowestCap(R.string.Market_Sorting_LowestCap),
    Gainers(R.string.Market_Sorting_Gainers),
    Losers(R.string.Market_Sorting_Losers);

    override val title = TranslatableString.ResString(titleResId)
}
