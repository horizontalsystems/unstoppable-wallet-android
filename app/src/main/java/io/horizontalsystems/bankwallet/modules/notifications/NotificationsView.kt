package io.horizontalsystems.bankwallet.modules.notifications

import androidx.lifecycle.MutableLiveData

class NotificationsView : NotificationsModule.IView {
    val itemsLiveData = MutableLiveData<List<NotificationsModule.PriceAlertViewItem>>()

    override fun setItems(items: List<NotificationsModule.PriceAlertViewItem>) {
        itemsLiveData.postValue(items)
    }
}
