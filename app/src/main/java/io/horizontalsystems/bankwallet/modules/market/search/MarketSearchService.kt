package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.CoinItem
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DataState
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DataState.Discovery
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DataState.SearchResult
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem.Category
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem.TopCoins
import io.horizontalsystems.marketkit.MarketKit
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class MarketSearchService(
    private val marketKit: MarketKit,
    private val marketFavoritesManager: MarketFavoritesManager,
) {
    private val disposables = CompositeDisposable()
    private var filter = ""

    private val discoveryItems by lazy {
        val discoveryItems: MutableList<MarketSearchModule.DiscoveryItem> = mutableListOf(TopCoins)

        marketKit.coinCategories().forEach {
            discoveryItems.add(Category(it))
        }

        discoveryItems
    }

    val stateObservable: BehaviorSubject<DataState> =
        BehaviorSubject.createDefault(Discovery(discoveryItems))

    init {
        marketFavoritesManager.dataUpdatedAsync
            .subscribeIO {
                syncState()
            }.let {
                disposables.add(it)
            }
    }

    private fun syncState() {
        if (filter.isBlank()) {
            stateObservable.onNext(Discovery(discoveryItems))
        } else {
            stateObservable.onNext(SearchResult(getCoinItems(filter)))
        }
    }

    private fun getCoinItems(filter: String): List<CoinItem> {
        return marketKit.fullCoins(filter).map {
            CoinItem(it, marketFavoritesManager.isCoinInFavorites(it.coin.uid))
        }
    }

    fun unFavorite(coinUid: String) {
        marketFavoritesManager.remove(coinUid)
    }

    fun favorite(coinUid: String) {
        marketFavoritesManager.add(coinUid)
    }

    fun setFilter(filter: String) {
        this.filter = filter
        syncState()
    }

    fun stop() {
        disposables.clear()
    }

}
