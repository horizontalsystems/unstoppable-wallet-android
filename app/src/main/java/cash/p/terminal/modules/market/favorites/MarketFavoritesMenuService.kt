package cash.p.terminal.modules.market.favorites

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.widgets.MarketWidgetManager

class MarketFavoritesMenuService(
    private val localStorage: ILocalStorage,
    private val marketWidgetManager: MarketWidgetManager
) {

    var sortingField: SortingField
        get() = localStorage.marketFavoritesSortingField ?: SortingField.HighestCap
        set(value) {
            localStorage.marketFavoritesSortingField = value
            marketWidgetManager.updateWatchListWidgets()
        }

    var marketField: MarketField
        get() = localStorage.marketFavoritesMarketField ?: MarketField.PriceDiff
        set(value) {
            localStorage.marketFavoritesMarketField = value
            marketWidgetManager.updateWatchListWidgets()
        }
}
