package cash.p.terminal.modules.market.category

import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.modules.market.MarketItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.sort
import cash.p.terminal.entities.Currency
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
            val marketInfoList = marketKit.marketInfosSingle(coinCategoryUid, baseCurrency.code).blockingGet()

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
