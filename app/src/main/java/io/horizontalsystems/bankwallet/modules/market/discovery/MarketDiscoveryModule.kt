package io.horizontalsystems.bankwallet.modules.market.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object MarketDiscoveryModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val provider = MarketCategoryProvider(App.instance)
            val service = MarketDiscoveryService(provider, App.currencyManager, App.xRateManager)
            return MarketDiscoveryViewModel(service, App.connectivityManager) as T
        }
    }

}
