package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinCategory
import io.horizontalsystems.marketkit.models.FullCoin

class MarketSearchService(private val marketKit: MarketKit) : Clearable {

    private val topCoinCategory = CoinCategory(
        "top_coins",
        "Top Coins",
        mapOf("en" to "Top Coins Description")
    )

    fun getCoinsByQuery(query: String): List<FullCoin> {
        return marketKit.fullCoins(query)
    }

    val coinCategories: List<CoinCategory>
        get() {
            val coinCategories = mutableListOf(topCoinCategory)
            coinCategories.addAll(marketKit.coinCategories())
            return coinCategories
        }

    override fun clear() = Unit
}
