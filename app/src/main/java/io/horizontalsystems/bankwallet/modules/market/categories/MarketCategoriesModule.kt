package io.horizontalsystems.bankwallet.modules.market.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object MarketCategoriesModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketCategoriesService(App.marketStorage)
            return MarketCategoriesViewModel(service) as T
        }

    }

}
