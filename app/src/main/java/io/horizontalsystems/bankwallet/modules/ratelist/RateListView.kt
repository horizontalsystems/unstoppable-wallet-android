package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.MutableLiveData

class RateListView : RateListModule.IView {

    var portfolioViewItemsLiveData = MutableLiveData<List<ViewItem>>()
    var topViewItemsLiveData = MutableLiveData<List<ViewItem>>()
    var datesLiveData = MutableLiveData<Long>()

    override fun setDate(lastUpdateTime: Long) {
        datesLiveData.postValue(lastUpdateTime)
    }

    override fun setPortfolioViewItems(viewItems: List<ViewItem>) {
        portfolioViewItemsLiveData.postValue(viewItems)
    }

    override fun setTopViewItems(viewItems: List<ViewItem>) {
        topViewItemsLiveData.postValue(viewItems)
    }
}
