package io.horizontalsystems.bankwallet.modules.market.platform

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.bankwallet.modules.market.topplatforms.Platform
import io.horizontalsystems.marketkit.models.MarketInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class MarketPlatformCoinsRepository(
    private val platform: Platform,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager
) {
    private var itemsCache: List<MarketInfo>? = null

    suspend fun get(
        sortingField: SortingField,
        timeDuration: TimeDuration,
        forceRefresh: Boolean,
        limit: Int? = null,
    ) = withContext(Dispatchers.IO) {
        val currentCache = itemsCache

        val items = if (forceRefresh || currentCache == null) {
            val currency = currencyManager.baseCurrency
            marketKit.topPlatformCoinListSingle(platform.uid, currency.code)
                .await()
        } else {
            currentCache
        }

        itemsCache = items

        val marketItems = getMarketItems(currencyManager.baseCurrency, items, timeDuration)

        marketItems.sort(sortingField).let { sortedList ->
            limit?.let { sortedList.take(it) } ?: sortedList
        }
    }

    private fun getMarketItems(
        currency: Currency,
        items: List<MarketInfo>?,
        timeDuration: TimeDuration
    ): List<MarketItem> {
        return items?.map { item ->
            val marketCapDiff = when (timeDuration) {
                TimeDuration.OneDay -> item.priceChange24h
                TimeDuration.SevenDay -> item.priceChange7d
                TimeDuration.ThirtyDay -> item.priceChange30d
                TimeDuration.ThreeMonths -> item.priceChange90d
            }

            MarketItem(
                fullCoin = item.fullCoin,
                volume = CurrencyValue(currency, item.totalVolume ?: BigDecimal.ZERO),
                rate = CurrencyValue(currency, item.price ?: BigDecimal.ZERO),
                diff = marketCapDiff,
                marketCap = CurrencyValue(currency, item.marketCap ?: BigDecimal.ZERO),
                rank = item.marketCapRank
            )
        } ?: emptyList()
    }

}
