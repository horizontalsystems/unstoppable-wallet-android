package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.CoinItem
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.Data.DiscoveryItems
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.Data.SearchResult
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem.Category
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem.TopCoins
import io.horizontalsystems.bankwallet.modules.market.sortedByDescendingNullLast
import io.horizontalsystems.bankwallet.modules.market.sortedByNullLast
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinCategoryMarketData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext

class MarketSearchService(
    private val marketKit: MarketKit,
    private val marketFavoritesManager: MarketFavoritesManager,
    private val baseCurrency: Currency,
) {

    private var marketData: List<CoinCategoryMarketData> = listOf()

    private var filter = ""

    private val periodOptions = TimeDuration.values().toList()

    private var selectedPeriod = periodOptions[0]

    private val _serviceDataFlow =
        MutableSharedFlow<Result<MarketSearchModule.Data>>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    val serviceDataFlow = _serviceDataFlow.asSharedFlow()

    val favoriteDataUpdated by marketFavoritesManager::dataUpdatedAsync

    val timePeriodMenu = Select(selectedPeriod, periodOptions)

    var sortDescending = true
        private set

    private val discoveryItems: List<MarketSearchModule.DiscoveryItem>
        get() {
            val items = marketKit.coinCategories().map { category ->
                Category(category, getCategoryMarketData(category.uid))
            }

            val sortedItems = if (sortDescending) {
                items.sortedByDescendingNullLast { it.marketData?.diff }
            } else {
                items.sortedByNullLast { it.marketData?.diff }
            }

            val discoveryItems: MutableList<MarketSearchModule.DiscoveryItem> =
                mutableListOf(TopCoins)

            discoveryItems.addAll(sortedItems)

            return discoveryItems
        }

    private val coinItems: List<CoinItem>
        get() {
            return marketKit.fullCoins(filter).map {
                CoinItem(it, marketFavoritesManager.isCoinInFavorites(it.coin.uid))
            }
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

            return MarketSearchModule.CategoryMarketData(marketCap ?: "----", diff)
        }
        return null
    }

    suspend fun updateState() = withContext(Dispatchers.IO) {
        try {
            if (filter.isBlank()) {
                if (marketData.isEmpty()) {
                    marketData =
                            marketKit.coinCategoriesMarketDataSingle(baseCurrency.code).await()
                }
                _serviceDataFlow.tryEmit(Result.success((DiscoveryItems(discoveryItems))))
            } else {
                _serviceDataFlow.tryEmit(Result.success((SearchResult(coinItems))))
            }
        } catch (e: Exception) {
            _serviceDataFlow.tryEmit(Result.failure(e))
        }
    }

    fun unFavorite(coinUid: String) {
        marketFavoritesManager.remove(coinUid)
    }

    fun favorite(coinUid: String) {
        marketFavoritesManager.add(coinUid)
    }

    suspend fun setFilter(filter: String) {
        this.filter = filter
        updateState()
    }

    suspend fun setTimePeriod(timeDuration: TimeDuration) {
        selectedPeriod = timeDuration
        updateState()
    }

    suspend fun toggleSortType() {
        sortDescending = !sortDescending
        updateState()
    }

}
