package io.horizontalsystems.bankwallet.modules.market.tabs

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable

class MarketTabsViewModel(private val service: MarketTabsService) : ViewModel() {

    val tabs by service::tabs
    var currentTab by service::currentTab
    val tabLiveData = MutableLiveData(currentTab)

    private val disposable = CompositeDisposable()

    init {
        service.currentTabChangedObservable
                .subscribe {
                    tabLiveData.postValue(service.currentTab)
                }
                .let {
                    disposable.add(it)
                }
    }

}
