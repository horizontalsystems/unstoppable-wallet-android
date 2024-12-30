package cash.p.terminal.modules.market.platform

import cash.p.terminal.wallet.MarketKitWrapper
import io.horizontalsystems.core.entities.CurrencyValue
import cash.p.terminal.modules.market.MarketItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.sort
import cash.p.terminal.modules.market.topplatforms.Platform
import io.horizontalsystems.core.CurrencyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class MarketPlatformCoinsRepository(
    private val platform: Platform,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager
) {
    private var itemsCache: List<MarketItem>? = null

    suspend fun get(
        sortingField: SortingField,
        forceRefresh: Boolean,
        limit: Int? = null,
    ) = withContext(Dispatchers.IO) {
        val currentCache = itemsCache

        val items = if (forceRefresh || currentCache == null) {
            val currency = currencyManager.baseCurrency
            val marketInfoItems = marketKit
                .topPlatformCoinListSingle(platform.uid, currency.code)
                .await()

            marketInfoItems.map { marketInfo ->
                MarketItem(
                    fullCoin = marketInfo.fullCoin,
                    volume = CurrencyValue(currency, marketInfo.totalVolume ?: BigDecimal.ZERO),
                    rate = CurrencyValue(currency, marketInfo.price ?: BigDecimal.ZERO),
                    diff = marketInfo.priceChange24h,
                    marketCap = CurrencyValue(currency, marketInfo.marketCap ?: BigDecimal.ZERO),
                    rank = marketInfo.marketCapRank
                )
            }
        } else {
            currentCache
        }

        itemsCache = items

        itemsCache?.sort(sortingField)?.let { sortedList ->
            limit?.let { sortedList.take(it) } ?: sortedList
        }
    }

}
