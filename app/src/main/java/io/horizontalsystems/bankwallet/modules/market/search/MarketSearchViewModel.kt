package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.ScreenState
import io.horizontalsystems.marketkit.models.FullCoin
import io.reactivex.disposables.CompositeDisposable

class MarketSearchViewModel(
    private val service: MarketSearchService
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private var coinItems = listOf<FullCoin>()

    val searchTextLiveData = MutableLiveData<String>()
    val screenStateLiveData = MutableLiveData<ScreenState>()

    init {
        service.stateObservable
            .subscribeIO {
                syncState(it)
            }.let {
                disposables.add(it)
            }

        service.marketFavoritesChangedObservable
            .subscribeIO {
                syncSearchResults()
            }.let {
                disposables.add(it)
            }
    }

    private fun syncState(dataState: MarketSearchModule.DataState) {
        val screenState = when (dataState) {
            is MarketSearchModule.DataState.Discovery -> {
                coinItems = listOf()
                ScreenState.Discovery(getDiscoveryViewItems(dataState.discoveryItems))
            }
            is MarketSearchModule.DataState.SearchResult -> {
                coinItems = dataState.coins
                ScreenState.SearchResult(getSearchResultViewItems(dataState.coins))
            }

        }
        screenStateLiveData.postValue(screenState)
    }

    private fun getSearchResultViewItems(coins: List<FullCoin>): List<MarketSearchModule.CoinViewItem> =
        coins.map {
            MarketSearchModule.CoinViewItem(it, service.isFavorite(it.coin.uid))
        }

    private fun getDiscoveryViewItems(items: List<MarketSearchModule.DiscoveryItem>): List<MarketSearchModule.CardViewItem> {
        return items.map {
            when (it) {
                is MarketSearchModule.DiscoveryItem.Category ->
                    MarketSearchModule.CardViewItem.MarketCoinCategory(it.coinCategory)
                is MarketSearchModule.DiscoveryItem.TopCoins ->
                    MarketSearchModule.CardViewItem.MarketTopCoins
            }
        }
    }

    private fun syncSearchResults() {
        if (coinItems.isNotEmpty() && screenStateLiveData.value is ScreenState.SearchResult) {
            screenStateLiveData.postValue(
                ScreenState.SearchResult(getSearchResultViewItems(coinItems)
                )
            )
        }
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun searchByQuery(query: String) {
        searchTextLiveData.postValue(query)
        service.setFilter(query)
    }

    fun onFavoriteClick(favorited: Boolean, coinUid: String) {
        if (favorited) {
            service.unFavorite(coinUid)
        } else {
            service.favorite(coinUid)
        }
    }

}
