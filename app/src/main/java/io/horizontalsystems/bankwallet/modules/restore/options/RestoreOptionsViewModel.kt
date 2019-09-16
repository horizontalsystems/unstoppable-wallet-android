package io.horizontalsystems.bankwallet.modules.restore.options

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.SyncMode

class RestoreOptionsViewModel : ViewModel(), RestoreOptionsModule.IView, RestoreOptionsModule.IRouter {

    lateinit var delegate: RestoreOptionsModule.IViewDelegate

    val notifySyncModeSelected = SingleLiveEvent<SyncMode>()
    val syncModeUpdatedLiveEvent = SingleLiveEvent<SyncMode>()

    fun init() {
        RestoreOptionsModule.init(this, this)
        delegate.viewDidLoad()
    }

    // View

    override fun update(syncMode: SyncMode) {
        syncModeUpdatedLiveEvent.value = syncMode
    }

    // Router

    override fun notifyOnSelect(syncMode: SyncMode) {
        notifySyncModeSelected.value = syncMode
    }
}
