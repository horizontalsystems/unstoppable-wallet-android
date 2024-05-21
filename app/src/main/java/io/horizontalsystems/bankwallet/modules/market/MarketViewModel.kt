package io.horizontalsystems.bankwallet.modules.market

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MarketViewModel(private val service: MarketService) : ViewModel() {

    val tabs = MarketModule.Tab.values()
    var selectedTab by mutableStateOf(getInitialTab())
        private set

    fun onSelect(tab: MarketModule.Tab) {
        service.currentTab = tab
        selectedTab = tab
    }

    private fun getInitialTab() = MarketModule.Tab.Watchlist
//        when (service.launchPage) {
//        LaunchPage.Market -> MarketModule.Tab.Overview
//        LaunchPage.Watchlist -> MarketModule.Tab.Watchlist
//        else -> service.currentTab ?: MarketModule.Tab.Overview
//    }
}
