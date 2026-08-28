package io.horizontalsystems.walletkit.modules.market.sector

import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.modules.market.MarketItem
import io.horizontalsystems.walletkit.modules.market.SortingField
import io.horizontalsystems.walletkit.modules.market.TimeDuration
import io.horizontalsystems.walletkit.modules.market.favorites.period
import io.horizontalsystems.walletkit.modules.market.sort
import io.horizontalsystems.marketkit.models.MarketInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min

class MarketSectorRepository(
    private val marketKit: MarketKitWrapper,
) {
    @Volatile
    private var cache: List<MarketInfo> = listOf()

    @Volatile
    private var cacheTimestamp: Long = 0
    private val cacheValidPeriodInMillis = 5_000 // 5 seconds

    private val mutex = Mutex()

    private suspend fun getMarketItems(coinCategoryUid: String, forceRefresh: Boolean, baseCurrency: Currency): List<MarketInfo> = mutex.withLock {
        if (forceRefresh && (cacheTimestamp + cacheValidPeriodInMillis < System.currentTimeMillis()) || cache.isEmpty()) {
            val marketInfoList = marketKit.marketInfosSingle(coinCategoryUid, baseCurrency.code)

            cache = marketInfoList
            cacheTimestamp = System.currentTimeMillis()

            marketInfoList
        } else {
            cache
        }
    }

    suspend fun get(
        coinCategoryUid: String,
        size: Int,
        sortingField: SortingField,
        timePeriod: TimeDuration,
        limit: Int,
        baseCurrency: Currency,
        forceRefresh: Boolean
    ): List<MarketItem> {
        val marketInfoItems = getMarketItems(coinCategoryUid, forceRefresh, baseCurrency)
        val marketItems = marketInfoItems.map { marketInfo ->
            MarketItem.createFromCoinMarket(marketInfo, baseCurrency, timePeriod.period)
        }
        return marketItems
            .subList(0, min(marketItems.size, size))
            .sort(sortingField)
            .subList(0, min(marketItems.size, limit))
    }
}
