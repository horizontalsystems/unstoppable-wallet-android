package io.horizontalsystems.bankwallet.modules.market

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent

class MarketViewModel(private val service: MarketService) : ViewModel() {

    private var currentTab = service.currentTab ?: MarketModule.Tab.Overview

    val tabs = MutableLiveData(Pair(MarketModule.Tab.values(), currentTab))
    val discoveryListTypeLiveEvent = SingleLiveEvent<MarketModule.ListType>()

    fun onSelect(tab: MarketModule.Tab) {
        service.currentTab = tab
        currentTab = tab
        tabs.postValue(Pair(MarketModule.Tab.values(), currentTab))
    }

    fun onClickSeeAll(listType: MarketModule.ListType) {
        discoveryListTypeLiveEvent.value = listType
        onSelect(MarketModule.Tab.Discovery)
    }

}
