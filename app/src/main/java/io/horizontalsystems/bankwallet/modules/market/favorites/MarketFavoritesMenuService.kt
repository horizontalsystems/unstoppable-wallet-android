package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesModule.Period
import io.horizontalsystems.bankwallet.widgets.MarketWidgetManager

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
