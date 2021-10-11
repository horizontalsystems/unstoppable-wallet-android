package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.CoinCategory

object MarketSearchModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketSearchService(App.marketKit)
            return MarketSearchViewModel(service, listOf(service)) as T
        }
    }

    sealed class ViewItem{
        object MarketTopCoins: ViewItem()
        data class MarketCoinCategory(val coinCategory: CoinCategory): ViewItem()
    }
}
