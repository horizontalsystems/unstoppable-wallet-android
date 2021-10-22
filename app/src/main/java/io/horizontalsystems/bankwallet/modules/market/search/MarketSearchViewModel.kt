package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.ScreenState
import io.reactivex.disposables.CompositeDisposable

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

    private val disposable = CompositeDisposable()

    val searchTextLiveData = MutableLiveData<String>()
    val screenStateLiveData = MutableLiveData<ScreenState>(ScreenState.CardsList(cards))


    fun searchByQuery(query: String) {
        searchTextLiveData.postValue(query)

        val queryTrimmed = query.trim()
        if (queryTrimmed.count() == 1) {
            screenStateLiveData.postValue(ScreenState.SearchResult(emptyList()))
        } else if (queryTrimmed.count() >= 2) {
            val results = service.getCoinsByQuery(queryTrimmed)
            if (results.isEmpty()) {
                screenStateLiveData.postValue(ScreenState.EmptySearchResult)
            } else {
                screenStateLiveData.postValue(ScreenState.SearchResult(results))
            }
        } else {
            screenStateLiveData.postValue(ScreenState.CardsList(cards))
        }
    }

    override fun onCleared() {
        disposable.clear()
    }
}
