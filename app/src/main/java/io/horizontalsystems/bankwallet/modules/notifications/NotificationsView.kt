package io.horizontalsystems.bankwallet.modules.notifications

import androidx.lifecycle.MutableLiveData

class NotificationsView : NotificationsModule.IView {
    val itemsLiveData = MutableLiveData<List<NotificationsModule.PriceAlertViewItem>>()
    val toggleWarningLiveData = MutableLiveData<Boolean>()

    override fun setItems(items: List<NotificationsModule.PriceAlertViewItem>) {
        itemsLiveData.postValue(items)
    }

    override fun showWarning() {
        toggleWarningLiveData.postValue(true)
    }

    override fun hideWarning() {
        toggleWarningLiveData.postValue(false)
    }
}
