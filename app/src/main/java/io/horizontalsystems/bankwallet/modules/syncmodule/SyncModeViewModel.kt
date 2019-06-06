package io.horizontalsystems.bankwallet.modules.syncmodule

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.entities.SyncMode

class SyncModeViewModel : ViewModel(), SyncModeModule.IView, SyncModeModule.IRouter, IKeyStoreSafeExecute {

    lateinit var delegate: SyncModeModule.IViewDelegate

    val errorLiveData = MutableLiveData<Int>()
    val navigateToSetPinLiveEvent = SingleLiveEvent<Void>()
    val syncModeUpdatedLiveEvent = SingleLiveEvent<SyncMode>()
    val showConfirmationDialogLiveEvent = SingleLiveEvent<Void>()
    val keyStoreSafeExecute = SingleLiveEvent<Triple<Runnable, Runnable?, Runnable?>>()

    fun init() {
        SyncModeModule.init(this, this, this)
        delegate.viewDidLoad()
    }

    override fun navigateToSetPin() {
        navigateToSetPinLiveEvent.call()
    }

    override fun updateSyncMode(syncMode: SyncMode) {
        syncModeUpdatedLiveEvent.value = syncMode
    }

    override fun safeExecute(action: Runnable, onSuccess: Runnable?, onFailure: Runnable?) {
        keyStoreSafeExecute.value = Triple(action, onSuccess, onFailure)
    }

    override fun showError(error: Int) {
        errorLiveData.value = error
    }

    override fun showConfirmationDialog() {
        showConfirmationDialogLiveEvent.call()
    }
}
