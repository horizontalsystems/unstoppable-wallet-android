package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.MutableLiveData
import java.util.*

class RateListView : RateListModule.IView {

    var viewItemsLiveData = MutableLiveData<List<ViewItem>>()
    var datesLiveData = MutableLiveData<Pair<Date, Long?>>()

    override fun setDates(date: Date, lastUpdateTime: Long?) {
        datesLiveData.postValue(Pair(date, lastUpdateTime))
    }

    override fun setViewItems(viewItems: List<ViewItem>) {
        viewItemsLiveData.postValue(viewItems)
    }
}
