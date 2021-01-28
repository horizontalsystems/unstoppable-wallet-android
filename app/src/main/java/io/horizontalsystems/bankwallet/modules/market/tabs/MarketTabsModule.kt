package io.horizontalsystems.bankwallet.modules.market.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object MarketTabsModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketTabsService(App.marketStorage)
            return MarketTabsViewModel(service) as T
        }

    }

}
