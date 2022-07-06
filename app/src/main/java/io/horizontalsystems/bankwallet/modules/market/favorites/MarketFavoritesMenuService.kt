package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.widgets.MarketWidgetManager

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
