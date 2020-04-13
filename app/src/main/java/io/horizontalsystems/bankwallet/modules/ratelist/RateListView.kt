package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.MutableLiveData
import java.util.*

class RateListView : RateListModule.IView {

    var portfolioViewItems = MutableLiveData<List<ViewItem>>()
    var topListViewItems = MutableLiveData<List<ViewItem>>()
    var datesLiveEvent = MutableLiveData<Pair<Date, Long?>>()

    override fun setDates(date: Date, lastUpdateTime: Long?) {
        datesLiveEvent.postValue(Pair(date, lastUpdateTime))
    }

    override fun showPortfolioItems(items: List<ViewItem>) {
        portfolioViewItems.postValue(items)
    }

    override fun showTopListItems(viewItems: List<ViewItem>) {
        topListViewItems.postValue(viewItems)
    }
}
