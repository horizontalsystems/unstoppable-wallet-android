package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.coinkit.models.CoinType

object MarketSearchModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketSearchService(App.xRateManager)
            return MarketSearchViewModel(service, listOf(service)) as T
        }
    }

}

data class CoinDataViewItem(val code: String, val name: String, val type: CoinType)
