package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.marketkit.models.CoinCategory

object MarketSearchModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketSearchService(App.marketKit)
            return MarketSearchViewModel(service, Translator, listOf(service)) as T
        }
    }

    data class ViewItem(
        val name: String,
        val description: String,
        val imageUrl: String,
        val type: ViewType
    )

    sealed class ViewType{
        object TopCoinsType: ViewType()
        data class CoinCategoryType(val coinCategory: CoinCategory): ViewType()
    }
}
