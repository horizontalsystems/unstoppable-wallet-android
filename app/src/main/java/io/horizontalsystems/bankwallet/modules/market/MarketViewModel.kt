package io.horizontalsystems.bankwallet.modules.market

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent

class MarketViewModel(private val service: MarketService) : ViewModel() {

    val tabs = MarketModule.Tab.values()
    var currentTabLiveData = MutableLiveData(service.currentTab ?: MarketModule.Tab.Overview)
    val discoveryListTypeLiveEvent = SingleLiveEvent<MarketModule.ListType>()

    fun onSelect(tabIndex: Int) {
        val selectedTab = tabs[tabIndex]
        service.currentTab = selectedTab
        currentTabLiveData.value = selectedTab
    }

    fun onClickSeeAll(listType: MarketModule.ListType) {
        discoveryListTypeLiveEvent.value = listType
        onSelect(tabs.indexOfFirst { it == MarketModule.Tab.Discovery })
    }

}
