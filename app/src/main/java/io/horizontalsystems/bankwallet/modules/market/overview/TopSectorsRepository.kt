package io.horizontalsystems.bankwallet.modules.market.overview

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem.Category
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinCategoryMarketData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TopSectorsRepository(
    private val marketKit: MarketKit,
) {
    private val itemsCount = 4
    private var itemsCache: List<Category>? = null

    suspend fun get(baseCurrency: Currency, forceRefresh: Boolean): List<Category> =
        withContext(Dispatchers.IO) {
            if (forceRefresh || itemsCache == null) {
                val coinCategoryData =
                    marketKit.coinCategoriesMarketDataSingle(baseCurrency.code).blockingGet()
                val discoveryItems = getDiscoveryItems(coinCategoryData, baseCurrency)
                itemsCache = discoveryItems
                itemsCache ?: emptyList()
            } else {
                itemsCache ?: emptyList()
            }
        }

    private fun getDiscoveryItems(marketData: List<CoinCategoryMarketData>, baseCurrency: Currency): List<Category> {
        val items = marketKit.coinCategories().map { category ->
            Category(
                category,
                getCategoryMarketData(marketData, category.uid, baseCurrency)
            )
        }

        return items
            .sortedByDescending { it.marketData?.diff }
            .take(itemsCount)
    }

    private fun getCategoryMarketData(
        marketData: List<CoinCategoryMarketData>,
        categoryUid: String,
        baseCurrency: Currency
    ): MarketSearchModule.CategoryMarketData? {
        marketData.firstOrNull { it.uid == categoryUid }?.let { coinCategoryMarketData ->
            val marketCap = coinCategoryMarketData.marketCap?.let { marketCap ->
                App.numberFormatter.formatFiatShort(marketCap, baseCurrency.symbol, 2)
            }

            if (marketCap != null) {
                return MarketSearchModule.CategoryMarketData(
                    marketCap,
                    coinCategoryMarketData.diff24H
                )
            }
        }
        return null
    }

}
