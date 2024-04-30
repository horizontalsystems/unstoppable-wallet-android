package cash.p.terminal.modules.market.favorites

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.modules.market.favorites.MarketFavoritesModule.Period
import cash.p.terminal.widgets.MarketWidgetManager

class MarketFavoritesMenuService(
    private val localStorage: ILocalStorage,
    private val marketWidgetManager: MarketWidgetManager
) {

    var sortDescending: Boolean
        get() = localStorage.marketFavoritesSortDescending
        set(value) {
            localStorage.marketFavoritesSortDescending = value
            marketWidgetManager.updateWatchListWidgets()
        }

    var period: Period
        get() = localStorage.marketFavoritesPeriod ?: Period.OneDay
        set(value) {
            localStorage.marketFavoritesPeriod = value
            marketWidgetManager.updateWatchListWidgets()
        }
}
