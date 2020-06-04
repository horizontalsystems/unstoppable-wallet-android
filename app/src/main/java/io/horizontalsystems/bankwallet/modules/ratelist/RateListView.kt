package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.MutableLiveData

class RateListView : RateListModule.IView {

    var portfolioViewItemsLiveData = MutableLiveData<List<CoinItem>>()
    var topViewItemsLiveData = MutableLiveData<List<CoinItem>>()
    var datesLiveData = MutableLiveData<Long>()

    override fun setDate(lastUpdateTime: Long) {
        datesLiveData.postValue(lastUpdateTime)
    }

    override fun setPortfolioViewItems(viewItems: List<CoinItem>) {
        portfolioViewItemsLiveData.postValue(viewItems)
    }

    override fun setTopViewItems(viewItems: List<CoinItem>) {
        topViewItemsLiveData.postValue(viewItems)
    }
}
