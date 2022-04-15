package io.horizontalsystems.bankwallet.modules.market.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.CoinItem
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DataState
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DataState.Discovery
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DataState.SearchResult
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem.Category
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem.TopCoins
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinCategoryMarketData
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MarketSearchService(
    private val marketKit: MarketKit,
    private val marketFavoritesManager: MarketFavoritesManager,
    private val baseCurrency: Currency,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val disposables = CompositeDisposable()
    private var topSectorsJob: Job? = null

    private var marketData: List<CoinCategoryMarketData> = listOf()
    private var filter = ""

    private val periodOptions = TimeDuration.values().toList()

    private var selectedPeriod = periodOptions[0]

    val timePeriodMenu = Select(selectedPeriod, periodOptions)

    var sortDescending = true
        private set

    var screenState by mutableStateOf<DataState>(Discovery(getDiscoveryItems()))
        private set

    private fun getDiscoveryItems(): List<MarketSearchModule.DiscoveryItem> {

        val items = marketKit.coinCategories().map { category ->
            Category(category, getCategoryMarketData(category.uid))
        }

        val sortedItems = if (sortDescending) {
            items.sortedByDescending { it.marketData?.diff }
        } else {
            items.sortedBy { it.marketData?.diff }
        }

        val discoveryItems: MutableList<MarketSearchModule.DiscoveryItem> =
            mutableListOf(TopCoins)

        discoveryItems.addAll(sortedItems)

        return discoveryItems
    }

    private fun getCategoryMarketData(categoryUid: String): MarketSearchModule.CategoryMarketData? {
        marketData.firstOrNull { it.uid == categoryUid }?.let { coinCategoryMarketData ->
            val marketCap = coinCategoryMarketData.marketCap?.let { marketCap ->
                val (shortenValue, suffix) = App.numberFormatter.shortenValue(marketCap)
                "$shortenValue$suffix"
            }

            val diff = when (selectedPeriod) {
                TimeDuration.OneDay -> coinCategoryMarketData.diff24H
                TimeDuration.SevenDay -> coinCategoryMarketData.diff1W
                TimeDuration.ThirtyDay -> coinCategoryMarketData.diff1M
            }

            if (marketCap!= null || diff != null){
                return MarketSearchModule.CategoryMarketData(marketCap, diff)
            }
        }
        return null
    }

    fun start() {
        marketFavoritesManager.dataUpdatedAsync
            .subscribeIO {
                syncState()
            }.let {
                disposables.add(it)
            }

        getDiscoveryMarketData()
    }

    private fun getDiscoveryMarketData() {
        topSectorsJob = coroutineScope.launch {
            val coinCategoryMarketData = marketKit.coinCategoriesMarketDataSingle(baseCurrency.code).blockingGet()
            marketData = coinCategoryMarketData
            syncState()
        }
    }

    private fun syncState() {
        if (filter.isBlank()) {
            screenState = Discovery(getDiscoveryItems())
        } else {
            screenState = SearchResult(getCoinItems(filter))
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

    fun setTimePeriod(timeDuration: TimeDuration) {
        selectedPeriod = timeDuration
        syncState()
    }

    fun toggleSortType() {
        sortDescending = !sortDescending
        syncState()
    }

}
