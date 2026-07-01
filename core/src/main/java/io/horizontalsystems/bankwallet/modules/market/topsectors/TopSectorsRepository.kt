package io.horizontalsystems.bankwallet.modules.market.topsectors

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.CoinCategory
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TopSectorsRepository(
    private val marketKit: MarketKitWrapper,
) {
    private var itemsCache: List<CoinCategoryWithTopCoins>? = null

    suspend fun get(baseCurrency: Currency, forceRefresh: Boolean): List<CoinCategoryWithTopCoins> =
        withContext(Dispatchers.IO) {
            if (forceRefresh || itemsCache == null) {
                val coinCategories = marketKit.coinCategoriesSingle(baseCurrency.code).blockingGet()
                itemsCache = coinCategories.map { coinCategory ->
                    val topCoins = marketKit
                        .fullCoins(coinUids = coinCategory.topCoins)
                        .sortedBy { fullCoin -> coinCategory.topCoins.indexOf(fullCoin.coin.uid) }
                    CoinCategoryWithTopCoins(coinCategory, topCoins)
                }
            }
            itemsCache ?: emptyList()
        }
}

data class CoinCategoryWithTopCoins(
    val coinCategory: CoinCategory,
    val topCoins: List<FullCoin>
)
