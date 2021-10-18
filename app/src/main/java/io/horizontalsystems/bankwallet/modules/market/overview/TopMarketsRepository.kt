package io.horizontalsystems.bankwallet.modules.market.overview

import io.horizontalsystems.bankwallet.modules.market.*
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.reactivex.Single
import kotlin.math.min

class TopMarketsRepository(
    private val marketKit: MarketKit
) {
    @Volatile
    private var cache: List<MarketItem> = listOf()

    @Volatile
    private var cacheTimestamp: Long = 0
    private val cacheValidPeriodInMillis = 5_000 // 5 seconds

    private val maxTopMarketSize = TopMarket.values().map { it.value }.maxOf { it }

    @Synchronized
    private fun getMarketItems(forceRefresh: Boolean, baseCurrency: Currency): List<MarketItem> =
        if (forceRefresh && (cacheTimestamp + cacheValidPeriodInMillis < System.currentTimeMillis()) || cache.isEmpty()) {
            val marketInfoList = marketKit.marketInfosSingle(maxTopMarketSize).blockingGet()

            val marketItems = marketInfoList.mapIndexed { index, marketInfo ->
                MarketItem.createFromCoinMarket(marketInfo, baseCurrency, Score.Rank(index + 1))
            }
            cache = marketItems
            cacheTimestamp = System.currentTimeMillis()

            marketItems
        } else {
            cache
        }

    fun get(
        size: Int,
        sortingField: SortingField,
        limit: Int,
        baseCurrency: Currency,
        forceRefresh: Boolean
    ): Single<List<MarketItem>> =
        Single.create { emitter ->
            try {
                val marketItems = getMarketItems(forceRefresh, baseCurrency)
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
