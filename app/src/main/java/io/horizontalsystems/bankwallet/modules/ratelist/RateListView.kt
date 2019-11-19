package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.MutableLiveData

class RateListView : RateListModule.IView {

    var rateListViewItem = MutableLiveData<RateListViewItem>()

    override fun show(item: RateListViewItem) {
        rateListViewItem.postValue(item)
    }
}
