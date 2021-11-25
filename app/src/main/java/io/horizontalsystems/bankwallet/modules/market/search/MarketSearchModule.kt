package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.CoinCategory
import io.horizontalsystems.marketkit.models.FullCoin
import javax.annotation.concurrent.Immutable

object MarketSearchModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketSearchService(App.marketKit, App.marketFavoritesManager)
            return MarketSearchViewModel(service) as T
        }
    }

    sealed class CardViewItem {
        object MarketTopCoins : CardViewItem()
        data class MarketCoinCategory(val coinCategory: CoinCategory) : CardViewItem()
    }

    sealed class DataState {
        class Discovery(val discoveryItems: List<DiscoveryItem>) : DataState()
        class SearchResult(val coins: List<FullCoin>) : DataState()
    }

    sealed class ScreenState {
        class Discovery(val cards: List<CardViewItem>) : ScreenState()
        class SearchResult(val coins: List<CoinViewItem>) : ScreenState()
    }

    sealed class DiscoveryItem {
        object TopCoins : DiscoveryItem()
        class Category(val coinCategory: CoinCategory) : DiscoveryItem()
    }

    @Immutable
    class CoinViewItem(val fullCoin: FullCoin, val favorited: Boolean)

}
