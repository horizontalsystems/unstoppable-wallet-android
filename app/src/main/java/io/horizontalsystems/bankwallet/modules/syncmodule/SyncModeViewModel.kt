package io.horizontalsystems.bankwallet.modules.syncmodule

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.SyncMode

class SyncModeViewModel : ViewModel(), SyncModeModule.IView, SyncModeModule.IRouter {

    lateinit var delegate: SyncModeModule.IViewDelegate

    val notifySyncModeSelected = SingleLiveEvent<SyncMode>()
    val syncModeUpdatedLiveEvent = SingleLiveEvent<SyncMode>()

    fun init() {
        SyncModeModule.init(this, this)
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
