package io.horizontalsystems.bankwallet.modules.market.category

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.sort
import io.reactivex.Single
import kotlin.math.min

class MarketCategoryRepository(
    private val marketKit: MarketKitWrapper,
) {
    @Volatile
    private var cache: List<MarketItem> = listOf()

    @Volatile
    private var cacheTimestamp: Long = 0
    private val cacheValidPeriodInMillis = 5_000 // 5 seconds

    @Synchronized
    private fun getMarketItems(coinCategoryUid: String, forceRefresh: Boolean, baseCurrency: Currency): List<MarketItem> =
        if (forceRefresh && (cacheTimestamp + cacheValidPeriodInMillis < System.currentTimeMillis()) || cache.isEmpty()) {
            val marketInfoList = marketKit.marketInfosSingle(coinCategoryUid, baseCurrency.code, "market_category").blockingGet()

            val marketItems = marketInfoList.map { marketInfo ->
                MarketItem.createFromCoinMarket(marketInfo, baseCurrency)
            }
            cache = marketItems
            cacheTimestamp = System.currentTimeMillis()

            marketItems
        } else {
            cache
        }

    fun get(
        coinCategoryUid: String,
        size: Int,
        sortingField: SortingField,
        limit: Int,
        baseCurrency: Currency,
        forceRefresh: Boolean
    ): Single<List<MarketItem>> =
        Single.create { emitter ->

            try {
                val marketItems = getMarketItems(coinCategoryUid, forceRefresh, baseCurrency)
                val sortedMarketItems = marketItems
                    .subList(0, min(marketItems.size, size))
                    .sort(sortingField)
                    .subList(0, min(marketItems.size, limit))

                emitter.onSuccess(sortedMarketItems)
            } catch (error: Throwable) {
                emitter.onError(error)
            }
        }
}
