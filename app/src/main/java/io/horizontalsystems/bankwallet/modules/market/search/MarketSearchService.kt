package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.FullCoin

class MarketSearchService(
    private val marketKit: MarketKit,
) {

    fun getCoinsByQuery(query: String): List<FullCoin> {
        return marketKit.fullCoins(query)
    }

    val coinCategories by lazy { marketKit.coinCategories() }

}
