package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import io.horizontalsystems.bankwallet.core.Clearable

class MarketAdvancedSearchService : Clearable {

    var coinList: CoinList = CoinList.Top250

    override fun clear() = Unit
}
