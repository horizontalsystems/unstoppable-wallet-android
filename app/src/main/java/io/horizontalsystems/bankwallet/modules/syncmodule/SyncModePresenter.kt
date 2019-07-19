package io.horizontalsystems.bankwallet.modules.syncmodule

import io.horizontalsystems.bankwallet.entities.SyncMode

class SyncModePresenter(private val router: SyncModeModule.IRouter, private val state: SyncModeModule.State)
    : SyncModeModule.IViewDelegate {

    var view: SyncModeModule.IView? = null

    override fun viewDidLoad() {
        view?.update(state.syncMode)
    }

    override fun onSyncModeSelect(isFast: Boolean) {
        state.syncMode = if (isFast) {
            SyncMode.FAST
        } else {
            SyncMode.SLOW
        }

        view?.update(state.syncMode)
    }

    override fun didConfirm() {
        router.notifyOnSelect(state.syncMode)
    }
}
