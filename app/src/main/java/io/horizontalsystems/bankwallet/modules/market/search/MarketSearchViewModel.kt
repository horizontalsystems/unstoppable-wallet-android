package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchViewModel.UiState
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class MarketSearchViewModel(
    private val marketFavoritesManager: MarketFavoritesManager,
    private val marketSearchService: MarketSearchService,
    private val marketDiscoveryService: MarketDiscoveryService,
) : ViewModelUiState<UiState>() {
    private var searchState = marketSearchService.stateFlow.value
    private var discoveryState = marketDiscoveryService.stateFlow.value
    private var listId: String = ""
    private var page: Page = Page.Discovery(
        recent = coinItems(discoveryState.recent),
        popular = coinItems(discoveryState.popular),
    )
    private var loading = true
    private var searchQuery = ""

    init {
        viewModelScope.launch {
            marketSearchService.stateFlow.collect {
                handleUpdatedSearchState(it)
            }
        }
        viewModelScope.launch {
            marketDiscoveryService.stateFlow.collect {
                handleUpdatedDiscoveryState(it)
            }
        }
        viewModelScope.launch {
            marketFavoritesManager.dataUpdatedAsync.asFlow().collect {
                syncState()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            marketDiscoveryService.start()
            loading = false
        }
    }

    override fun createState(): UiState {
        return UiState(
            page = page,
            listId = listId,
            loading = loading,
            searchQuery = searchQuery,
        )
    }

    private fun handleUpdatedDiscoveryState(discoveryState: MarketDiscoveryService.State) {
        this.discoveryState = discoveryState

        syncState()
    }

    private fun handleUpdatedSearchState(searchState: MarketSearchService.State) {
        this.searchState = searchState

        syncState()
    }

    fun searchByQuery(query: String) {
        searchQuery = query
        marketSearchService.setQuery(query)
    }

    private fun coinItems(fullCoins: List<FullCoin>) =
        fullCoins.map {
            MarketSearchModule.CoinItem(
                it,
                marketFavoritesManager.isCoinInFavorites(it.coin.uid)
            )
        }

    private fun syncState() {
        if (searchState.query.isNotBlank()) {
            page = Page.SearchResults(coinItems(searchState.results))
            listId = searchState.query
        } else {
            page = Page.Discovery(
                coinItems(discoveryState.recent),
                coinItems(discoveryState.popular),
            )
            listId = ""
        }

        viewModelScope.launch {
            emitState()
        }
    }

    fun onFavoriteClick(favourited: Boolean, coinUid: String) {
        if (favourited) {
            marketFavoritesManager.remove(coinUid)

            stat(page = StatPage.MarketSearch, event = StatEvent.RemoveFromWatchlist(coinUid))
        } else {
            marketFavoritesManager.add(coinUid)

            stat(page = StatPage.MarketSearch, event = StatEvent.AddToWatchlist(coinUid))
        }
    }

    fun onCoinOpened(coin: Coin) {
        marketDiscoveryService.addCoinToRecent(coin)
    }

    data class UiState(
        val page: Page,
        val listId: String,
        val loading: Boolean,
        val searchQuery: String,
    )

    sealed class Page {
        data class Discovery(
            val recent: List<MarketSearchModule.CoinItem>,
            val popular: List<MarketSearchModule.CoinItem>
        ) : Page()

        data class SearchResults(val items: List<MarketSearchModule.CoinItem>) : Page()
    }
}
