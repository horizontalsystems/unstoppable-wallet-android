package cash.p.terminal.modules.market.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.wallet.models.CoinCategory
import cash.p.terminal.wallet.entities.FullCoin
import java.math.BigDecimal
import javax.annotation.concurrent.Immutable

object MarketSearchModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MarketSearchViewModel(
                App.marketFavoritesManager,
                MarketSearchService(App.marketKit),
                MarketDiscoveryService(App.marketKit, App.localStorage),
            ) as T
        }
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
