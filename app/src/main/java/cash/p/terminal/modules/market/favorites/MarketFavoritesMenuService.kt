package cash.p.terminal.modules.market.favorites

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.widgets.MarketWidgetManager

class MarketFavoritesMenuService(
    private val localStorage: ILocalStorage,
    private val marketWidgetManager: MarketWidgetManager
) {

    var listSorting: WatchlistSorting
        get() = localStorage.marketFavoritesSorting ?: WatchlistSorting.Manual
        set(value) {
            localStorage.marketFavoritesSorting = value
            marketWidgetManager.updateWatchListWidgets()
        }

    var timeDuration: TimeDuration
        get() = localStorage.marketFavoritesPeriod ?: TimeDuration.OneDay
        set(value) {
            localStorage.marketFavoritesPeriod = value
            marketWidgetManager.updateWatchListWidgets()
        }

    var showSignals: Boolean
        get() = localStorage.marketFavoritesShowSignals
        set(value) {
            localStorage.marketFavoritesShowSignals = value
            marketWidgetManager.updateWatchListWidgets()
        }

    var manualSortOrder: List<String>
        get() = localStorage.marketFavoritesManualSortingOrder
        set(value) {
            localStorage.marketFavoritesManualSortingOrder = value
            marketWidgetManager.updateWatchListWidgets()
        }
}
