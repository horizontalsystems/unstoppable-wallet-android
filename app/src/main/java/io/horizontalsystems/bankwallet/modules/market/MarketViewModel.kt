package io.horizontalsystems.bankwallet.modules.market

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent

class MarketViewModel(private val service: MarketService) : ViewModel() {

    val tabs = MarketModule.Tab.values()
    val selectedTab = MutableLiveData(service.currentTab ?: MarketModule.Tab.Overview)
    val discoveryListTypeLiveEvent = SingleLiveEvent<MarketModule.ListType>()

    fun onSelect(tab: MarketModule.Tab) {
        service.currentTab = tab
        selectedTab.postValue(tab)
    }

    fun onClickSeeAll(listType: MarketModule.ListType) {
        discoveryListTypeLiveEvent.value = listType
        onSelect(MarketModule.Tab.Discovery)
    }

}
