package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.MutableLiveData
import java.util.*

class RateListView : RateListModule.IView {

    var viewItemsLiveData = MutableLiveData<List<ViewItem>>()
    var datesLiveData = MutableLiveData<Long>()

    override fun setDate(lastUpdateTime: Long) {
        datesLiveData.postValue(lastUpdateTime)
    }

    override fun setViewItems(viewItems: List<ViewItem>) {
        viewItemsLiveData.postValue(viewItems)
    }
}
