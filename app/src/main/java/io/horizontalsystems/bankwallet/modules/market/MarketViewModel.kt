package io.horizontalsystems.bankwallet.modules.market

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.core.SingleLiveEvent

class MarketViewModel(private val service: MarketService) : ViewModel() {

    val tabs = MarketModule.Tab.values()
    val selectedTab = MutableLiveData(getSelectedTab())
    val discoveryListTypeLiveEvent = SingleLiveEvent<MarketModule.ListType>()

    fun onSelect(tab: MarketModule.Tab) {
        service.currentTab = tab
        selectedTab.postValue(tab)
    }

    fun onClickSeeAll(listType: MarketModule.ListType) {
        discoveryListTypeLiveEvent.value = listType
//        onSelect(MarketModule.Tab.Posts)
    }

    private fun getSelectedTab(): MarketModule.Tab {
        return when (service.launchPage) {
            LaunchPage.Market -> MarketModule.Tab.Overview
            LaunchPage.Watchlist -> MarketModule.Tab.Watchlist
            else -> service.currentTab ?: MarketModule.Tab.Overview
        }
    }

}
