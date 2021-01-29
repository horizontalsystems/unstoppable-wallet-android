package io.horizontalsystems.bankwallet.modules.market

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable

class MarketViewModel(private val service: MarketService) : ViewModel() {

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
