package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.FullCoin

class MarketSearchService(
    private val marketKit: MarketKit,
) : Clearable {

    fun getCoinsByQuery(query: String): List<FullCoin> {
        return marketKit.fullCoins(query)
    }

    val coinCategories by lazy { marketKit.coinCategories() }

    override fun clear() = Unit
}
