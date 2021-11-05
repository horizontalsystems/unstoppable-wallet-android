package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.list.IMarketListMenu

class MarketAdvancedSearchMenuService: IMarketListMenu {

    override var sortingField = SortingField.HighestCap

    override var marketField = MarketField.PriceDiff

}
