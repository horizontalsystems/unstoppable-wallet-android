package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinCategory
import io.horizontalsystems.marketkit.models.FullCoin

class MarketSearchService(
    private val marketKit: MarketKit,
    translator: Translator
) : Clearable {

    private val topCoinCategory = CoinCategory(
        "top_coins",
        translator.getString(R.string.Market_Category_TopCoins),
        mapOf("en" to translator.getString(R.string.Market_Category_TopCoins_Description))
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
