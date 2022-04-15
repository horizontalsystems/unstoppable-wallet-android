package io.horizontalsystems.bankwallet.modules.market.overview

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem.Category
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinCategoryMarketData

class TopSectorsRepository(
    private val marketKit: MarketKit,
) {
    private val itemsCount = 4
    private var itemsCache: List<Category>? = null

    suspend fun get(baseCurrency: Currency, forceRefresh: Boolean): List<Category> =
        if (forceRefresh || itemsCache == null) {
            val coinCategoryData = marketKit.coinCategoriesMarketData(baseCurrency.code)
            val discoveryItems = getDiscoveryItems(coinCategoryData)
            itemsCache = discoveryItems
            itemsCache ?: emptyList()
        } else {
            itemsCache ?: emptyList()
        }

    private fun getDiscoveryItems(marketData: List<CoinCategoryMarketData>): List<Category> {
        val items = marketKit.coinCategories().map { category ->
            Category(
                category,
                getCategoryMarketData(marketData, category.uid)
            )
        }

        return items
            .sortedByDescending { it.marketData?.diff }
            .take(itemsCount)
    }

    private fun getCategoryMarketData(
        marketData: List<CoinCategoryMarketData>,
        categoryUid: String
    ): MarketSearchModule.CategoryMarketData? {
        marketData.firstOrNull { it.uid == categoryUid }?.let { coinCategoryMarketData ->
            val marketCap = coinCategoryMarketData.marketCap?.let { marketCap ->
                val (shortenValue, suffix) = App.numberFormatter.shortenValue(marketCap)
                "$shortenValue$suffix"
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
