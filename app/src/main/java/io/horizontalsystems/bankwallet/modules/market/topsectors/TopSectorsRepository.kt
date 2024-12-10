package io.horizontalsystems.bankwallet.modules.market.topsectors

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.CoinCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TopSectorsRepository(
    private val marketKit: MarketKitWrapper,
) {
    private var itemsCache: List<CoinCategory>? = null

    suspend fun get(baseCurrency: Currency, forceRefresh: Boolean): List<CoinCategory> =
        withContext(Dispatchers.IO) {
            if (forceRefresh || itemsCache == null) {
                val coinCategories = marketKit.coinCategoriesSingle(baseCurrency.code).blockingGet()
                itemsCache = coinCategories
                itemsCache ?: emptyList()
            } else {
                itemsCache ?: emptyList()
            }
        }
}
