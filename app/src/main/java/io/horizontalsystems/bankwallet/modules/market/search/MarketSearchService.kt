package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DataState
import io.horizontalsystems.marketkit.MarketKit
import io.reactivex.subjects.BehaviorSubject

class MarketSearchService(
    private val marketKit: MarketKit,
    private val marketFavoritesManager: MarketFavoritesManager,
) {

    private val discoveryItems by lazy {
        val discoveryItems: MutableList<MarketSearchModule.DiscoveryItem> =
            mutableListOf(MarketSearchModule.DiscoveryItem.TopCoins)

        marketKit.coinCategories().forEach {
            discoveryItems.add(MarketSearchModule.DiscoveryItem.Category(it))
        }

        discoveryItems
    }

    val marketFavoritesChangedObservable by marketFavoritesManager::dataUpdatedAsync
    val stateObservable: BehaviorSubject<DataState> = BehaviorSubject.createDefault(
        DataState.Discovery(discoveryItems)
    )

    fun isFavorite(coinUid: String): Boolean = marketFavoritesManager.isCoinInFavorites(coinUid)

    fun unFavorite(coinUid: String) {
        marketFavoritesManager.remove(coinUid)
    }

    fun favorite(coinUid: String) {
        marketFavoritesManager.add(coinUid)
    }

    fun setFilter(filter: String) {
        if (filter.isEmpty()) {
            stateObservable.onNext(DataState.Discovery(discoveryItems))
        } else {
            stateObservable.onNext(DataState.SearchResult(marketKit.fullCoins(filter)))
        }
    }

}
