package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DataState
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
        val discoveryItems: MutableList<MarketSearchModule.DiscoveryItem> =
            mutableListOf(MarketSearchModule.DiscoveryItem.TopCoins)

        marketKit.coinCategories().forEach {
            discoveryItems.add(MarketSearchModule.DiscoveryItem.Category(it))
        }

        discoveryItems
    }

    val stateObservable: BehaviorSubject<DataState> = BehaviorSubject.createDefault(
        DataState.Discovery(discoveryItems)
    )

    init {
        marketFavoritesManager.dataUpdatedAsync
            .subscribeIO {
                syncState()
            }.let {
                disposables.add(it)
            }
    }

    private fun syncState() {
        if (filter.isEmpty()) {
            stateObservable.onNext(DataState.Discovery(discoveryItems))
        } else {
            stateObservable.onNext(DataState.SearchResult(getCoinViewItems(filter)))
        }
    }

    private fun getCoinViewItems(filter: String): List<MarketSearchModule.CoinViewItem> {
        return marketKit.fullCoins(filter).map {
            MarketSearchModule.CoinViewItem(it, marketFavoritesManager.isCoinInFavorites(it.coin.uid))
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
