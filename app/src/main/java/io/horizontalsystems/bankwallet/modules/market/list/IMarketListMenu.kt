package io.horizontalsystems.bankwallet.modules.market.list

import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField

interface IMarketListMenu {
    var sortingField: SortingField
    var marketField: MarketField
}
