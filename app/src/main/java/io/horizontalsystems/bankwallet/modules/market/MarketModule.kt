package io.horizontalsystems.bankwallet.modules.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

object MarketModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val marketService = MarketCategoriesService()
            return MarketCategoriesViewModel(marketService) as T
        }

    }

}
