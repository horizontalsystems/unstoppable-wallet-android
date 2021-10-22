package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.ScreenState
import io.horizontalsystems.marketkit.models.FullCoin

class MarketSearchViewModel(
    private val service: MarketSearchService
) : ViewModel() {

    private val cards by lazy {
        val coinCategories = service.coinCategories
        val items = coinCategories.map { category ->
            MarketSearchModule.CardViewItem.MarketCoinCategory(category)
        }
        val topCoins = MarketSearchModule.CardViewItem.MarketTopCoins
        listOf(topCoins) + items
    }

    private var coinItems = listOf<FullCoin>()
    private val favoritedCoinUids = service.favoritedCoinUids.toMutableList()

    val searchTextLiveData = MutableLiveData<String>()
    val screenStateLiveData = MutableLiveData<ScreenState>(ScreenState.CardsList(cards))


    fun searchByQuery(query: String) {
        searchTextLiveData.postValue(query)

        coinItems = emptyList()
        val queryTrimmed = query.trim()
        if (queryTrimmed.count() == 1) {
            screenStateLiveData.postValue(ScreenState.SearchResult(emptyList()))
        } else if (queryTrimmed.count() >= 2) {
            val results = service.getCoinsByQuery(queryTrimmed)
            if (results.isEmpty()) {
                screenStateLiveData.postValue(ScreenState.EmptySearchResult)
            } else {
                coinItems = results
                screenStateLiveData.postValue(ScreenState.SearchResult(getCoinViewItems()))
            }
        } else {
            screenStateLiveData.postValue(ScreenState.CardsList(cards))
        }
    }

    fun onFavoriteClick(favourited: Boolean, coinUid: String) {
        if (favourited) {
            favoritedCoinUids.remove(coinUid)
            service.unFavorite(coinUid)
        } else {
            favoritedCoinUids.add(coinUid)
            service.favorite(coinUid)
        }
        screenStateLiveData.postValue(ScreenState.SearchResult(getCoinViewItems()))
    }

    private fun getCoinViewItems() =
        coinItems.map {
            MarketSearchModule.CoinViewItem(
                it,
                favoritedCoinUids.contains(it.coin.uid)
            )
        }

}
