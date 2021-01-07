package io.horizontalsystems.bankwallet.modules.market.defi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.top.MarketTopService
import io.horizontalsystems.bankwallet.modules.market.top.MarketTopViewModel

object MarketDefiModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketTopService(App.currencyManager, MarketListDefiDataSource(App.xRateManager))
            return MarketTopViewModel(service, listOf(service)) as T
        }

    }
}
