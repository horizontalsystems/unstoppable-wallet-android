package io.horizontalsystems.bankwallet.modules.market.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.MarketModule

object MarketDiscoveryModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val provider = MarketCategoryProvider(App.instance)
            val service = MarketDiscoveryService(provider, MarketModule.currencyUSD, App.xRateManager)
            return MarketDiscoveryViewModel(service, App.connectivityManager, listOf(service)) as T
        }
    }

}
