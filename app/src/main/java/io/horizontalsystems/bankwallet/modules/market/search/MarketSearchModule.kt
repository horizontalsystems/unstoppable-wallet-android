package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.CoinCategory
import io.horizontalsystems.marketkit.models.FullCoin

object MarketSearchModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketSearchService(App.marketKit, App.marketFavoritesManager)
            return MarketSearchViewModel(service) as T
        }
    }

    sealed class CardViewItem{
        object MarketTopCoins: CardViewItem()
        data class MarketCoinCategory(val coinCategory: CoinCategory): CardViewItem()
    }

    sealed class ScreenState {
        class CardsList(val cards: List<CardViewItem>) : ScreenState()
        class SearchResult(val coins: List<CoinViewItem>) : ScreenState()
        object EmptySearchResult : ScreenState()
    }

    class CoinViewItem(val fullCoin: FullCoin, val favorited: Boolean)
}
