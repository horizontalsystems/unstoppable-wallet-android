package io.horizontalsystems.bankwallet.modules.contact.appstatus

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.core.SingleLiveEvent

class AppStatusView : AppStatusModule.IView {

    val appStatusLiveData = MutableLiveData<Map<String, Any>>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()

    override fun setAppStatus(status: Map<String, Any>) {
        appStatusLiveData.postValue(status)
    }

    override fun showCopied() {
        showCopiedLiveEvent.postValue(Unit)
    }

}
