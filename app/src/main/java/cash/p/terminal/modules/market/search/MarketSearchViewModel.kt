package cash.p.terminal.modules.market.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.MarketKitWrapper
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext

class MarketSearchViewModel(
    private val marketKit: MarketKitWrapper,
    private val marketFavoritesManager: MarketFavoritesManager,
    private val localStorage: ILocalStorage,
) : ViewModel() {

    enum class Mode {
        Loading, Discovery, SearchResults
    }

    private var popularFullCoins: List<FullCoin>? = null

    private var results: List<MarketSearchModule.CoinItem>? = null
    private var recent: List<MarketSearchModule.CoinItem>? = null
    private var popular: List<MarketSearchModule.CoinItem>? = null
    private var query: String = ""
    private var mode: Mode = Mode.Loading

    var uiState by mutableStateOf(
        UiState(
            mode = mode,
            recent = recent,
            popular = popular,
            results = results,
        )
    )
        private set

    init {
        viewModelScope.launch {
            marketFavoritesManager.dataUpdatedAsync.asFlow().collect {
                refreshItems()
                emitState()
            }
        }

        viewModelScope.launch {
            fetchDiscoveryItems()
        }
    }

    private suspend fun fetchDiscoveryItems() = withContext(Dispatchers.IO) {
        popularFullCoins = marketKit.fullCoins(filter = "")

        if (mode == Mode.Loading) {
            mode = Mode.Discovery
            val recentFullCoins = marketKit
                .fullCoins(localStorage.marketSearchRecentCoinUids)
                .sortedBy {
                    localStorage.marketSearchRecentCoinUids.indexOf(it.coin.uid)
                }
            recent = coinItems(recentFullCoins)
            popular = popularFullCoins?.let { coinItems(it) }
            emitState()
        }
    }

    fun searchByQuery(query: String) {
        this.query = query

        refreshItems()
        emitState()
    }

    private fun refreshItems() {
        if (query.isBlank()) {
            mode = Mode.Discovery
            val recentFullCoins = marketKit
                .fullCoins(localStorage.marketSearchRecentCoinUids)
                .sortedBy {
                    localStorage.marketSearchRecentCoinUids.indexOf(it.coin.uid)
                }
            recent = coinItems(recentFullCoins)
            popular = popularFullCoins?.let { coinItems(it) }
            results = null
        } else {
            mode = Mode.SearchResults
            recent = null
            popular = null
            results = coinItems(marketKit.fullCoins(query))
        }
    }

    private fun coinItems(fullCoins: List<FullCoin>) =
        fullCoins.map {
            MarketSearchModule.CoinItem(
                it,
                marketFavoritesManager.isCoinInFavorites(it.coin.uid)
            )
        }

    private fun emitState() {
        viewModelScope.launch {
            uiState = UiState(mode, recent, popular, results)
        }
    }

    fun onFavoriteClick(favourited: Boolean, coinUid: String) {
        if (favourited) {
            marketFavoritesManager.remove(coinUid)
        } else {
            marketFavoritesManager.add(coinUid)
        }
    }

    fun onCoinOpened(coin: Coin) {
        localStorage.marketSearchRecentCoinUids =
            (listOf(coin.uid) + localStorage.marketSearchRecentCoinUids).distinct().take(5)
    }

    data class UiState(
        val mode: Mode,
        val recent: List<MarketSearchModule.CoinItem>?,
        val popular: List<MarketSearchModule.CoinItem>?,
        val results: List<MarketSearchModule.CoinItem>?
    )
}
