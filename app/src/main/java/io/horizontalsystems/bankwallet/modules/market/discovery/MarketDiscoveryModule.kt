package io.horizontalsystems.bankwallet.modules.market.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.list.MarketListService
import io.horizontalsystems.bankwallet.modules.market.list.MarketListViewModel

object MarketDiscoveryModule {

    class Factory : ViewModelProvider.Factory {
        val provider by lazy { MarketCategoryProvider(App.instance) }
        val service by lazy { MarketDiscoveryService(provider, App.xRateManager) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) = when (modelClass) {
            MarketDiscoveryViewModel::class.java -> {
                MarketDiscoveryViewModel(service) as T
            }
            MarketListViewModel::class.java -> {
                val listService = MarketListService(MarketModule.currencyUSD, service)
                MarketListViewModel(listService, App.connectivityManager, listOf(listService)) as T
            }
            else -> throw IllegalArgumentException()
        }
    }

}
