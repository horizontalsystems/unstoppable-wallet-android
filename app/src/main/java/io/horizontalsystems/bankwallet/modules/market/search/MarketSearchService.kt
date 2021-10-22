package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.FullCoin

class MarketSearchService(
    private val marketKit: MarketKit,
    private val marketFavoritesManager: MarketFavoritesManager,
) {

    val favoritedCoinUids = marketFavoritesManager.getAll().map { it.coinUid }

    fun getCoinsByQuery(query: String): List<FullCoin> {
        return marketKit.fullCoins(query)
    }

    fun unFavorite(coinUid: String) {
        marketFavoritesManager.remove(coinUid)
    }

    fun favorite(coinUid: String) {
        marketFavoritesManager.add(coinUid)
    }

    val coinCategories by lazy { marketKit.coinCategories() }

}
