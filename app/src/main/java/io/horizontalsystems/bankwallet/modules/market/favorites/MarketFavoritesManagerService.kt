package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager

class MarketFavoritesManagerService(
    private val favoritesManager: MarketFavoritesManager
) {

    val dataUpdated by favoritesManager::dataUpdatedAsync

    val allFavorites: HashSet<String>
        get() = favoritesManager.getAll()
            .map { it.coinUid }
            .toHashSet()

    fun addFavorite(uid: String) {
        favoritesManager.add(uid)
    }

    fun removeFavorite(uid: String) {
        favoritesManager.remove(uid)
    }
}
