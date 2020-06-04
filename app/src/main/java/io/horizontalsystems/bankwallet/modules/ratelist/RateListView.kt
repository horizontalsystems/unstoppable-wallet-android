package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.MutableLiveData

class RateListView : RateListModule.IView {

    var portfolioViewItemsLiveData = MutableLiveData<List<CoinViewItem>>()
    var topViewItemsLiveData = MutableLiveData<List<CoinViewItem>>()
    var datesLiveData = MutableLiveData<Long>()

    override fun setDate(lastUpdateTime: Long) {
        datesLiveData.postValue(lastUpdateTime)
    }

    override fun setPortfolioViewItems(viewItems: List<CoinViewItem>) {
        portfolioViewItemsLiveData.postValue(viewItems)
    }

    override fun setTopViewItems(viewItems: List<CoinViewItem>) {
        topViewItemsLiveData.postValue(viewItems)
    }
}
