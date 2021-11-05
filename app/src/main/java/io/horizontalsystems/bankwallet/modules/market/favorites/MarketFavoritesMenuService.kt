package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.list.IMarketListMenu

class MarketFavoritesMenuService(private val localStorage: ILocalStorage): IMarketListMenu {

    override var sortingField: SortingField
        get() = localStorage.marketFavoritesSortingField ?: SortingField.HighestCap
        set(value) {
            localStorage.marketFavoritesSortingField = value
        }

    override var marketField: MarketField
        get() = localStorage.marketFavoritesMarketField ?: MarketField.PriceDiff
        set(value) {
            localStorage.marketFavoritesMarketField = value
        }
}
