package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class RestoreView : RestoreModule.IView {

    val reloadLiveEvent = SingleLiveEvent<List<PredefinedAccountType>>()
    val showErrorLiveEvent = SingleLiveEvent<Exception>()

    override fun reload(items: List<PredefinedAccountType>) {
        reloadLiveEvent.postValue(items)
    }

    override fun showError(ex: Exception) {
        showErrorLiveEvent.postValue(ex)
    }

}
