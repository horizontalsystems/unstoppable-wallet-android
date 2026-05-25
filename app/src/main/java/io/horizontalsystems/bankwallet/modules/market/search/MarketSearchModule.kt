package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.marketkit.models.CoinCategory
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal
import javax.annotation.concurrent.Immutable

object MarketSearchModule {

    sealed class DiscoveryItem {
        object TopCoins : DiscoveryItem()

        class Category(
            val coinCategory: CoinCategory,
            val marketData: CategoryMarketData? = null
        ) : DiscoveryItem()
    }

    @Immutable
    class CoinItem(val fullCoin: FullCoin, val favourited: Boolean)

    data class CategoryMarketData(
        val marketCap: String? = null,
        val diff: BigDecimal? = null
    )

}
