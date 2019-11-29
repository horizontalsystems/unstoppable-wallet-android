package io.horizontalsystems.bankwallet.modules.restore

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class RestoreViewModel : ViewModel(), RestoreModule.View, RestoreModule.Router {

    lateinit var delegate: RestoreModule.ViewDelegate

    val reloadLiveEvent = SingleLiveEvent<List<PredefinedAccountType>>()
    val showErrorLiveEvent = SingleLiveEvent<Exception>()
    val startRestoreCoins = SingleLiveEvent<PredefinedAccountType>()
    val closeLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        RestoreModule.init(this, this)
        delegate.viewDidLoad()
    }

    //  View

    override fun reload(items: List<PredefinedAccountType>) {
        reloadLiveEvent.postValue(items)
    }

    override fun showError(ex: Exception) {
        showErrorLiveEvent.postValue(ex)
    }

    //  Router
    override fun startRestoreCoins(predefinedAccountType: PredefinedAccountType) {
        startRestoreCoins.postValue(predefinedAccountType)
    }

    override fun close() {
        closeLiveEvent.call()
    }
}
