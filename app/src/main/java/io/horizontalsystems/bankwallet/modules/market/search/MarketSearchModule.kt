package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.CoinCategory
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal
import javax.annotation.concurrent.Immutable

object MarketSearchModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = MarketSearchService(
                App.marketKit,
                App.marketFavoritesManager,
                App.currencyManager.baseCurrency
            )
            return MarketSearchViewModel(service) as T
        }
    }

    sealed class Data {
        class DiscoveryItems(val discoveryItems: List<DiscoveryItem>) : Data()
        class SearchResult(val coinItems: List<CoinItem>) : Data()
    }

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
