package io.horizontalsystems.bankwallet.modules.restore

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType

class RestoreViewModel : ViewModel(), RestoreModule.View, RestoreModule.Router {

    lateinit var delegate: RestoreModule.ViewDelegate

    val reloadLiveEvent = SingleLiveEvent<List<IPredefinedAccountType>>()
    val startRestoreWordsLiveEvent = SingleLiveEvent<Unit>()
    val closeLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        RestoreModule.init(this, this)
        delegate.viewDidLoad()
    }

    // Router

    override fun reload(items: List<IPredefinedAccountType>) {
        reloadLiveEvent.postValue(items)
    }

    override fun startRestoreWordsModule() {
        startRestoreWordsLiveEvent.call()
    }

    override fun close() {
        closeLiveEvent.call()
    }
}
