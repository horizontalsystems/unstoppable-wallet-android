package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager

class MarketFavoritesToggleService(
    private val favoritesManager: MarketFavoritesManager
) {

    val allFavorites: HashSet<String>
        get() = favoritesManager.getAll()
            .map { it.coinUid }
            .toHashSet()

    fun toggleCoinFavorite(uid: String) {
        val inFavorites = favoritesManager.isCoinInFavorites(uid)
        if (inFavorites) {
            favoritesManager.remove(uid)
        } else {
            favoritesManager.add(uid)
        }
    }

    fun remove(uid: String) {
        favoritesManager.remove(uid)
    }
}
