package io.horizontalsystems.bankwallet.modules.restore.options

import io.horizontalsystems.bankwallet.entities.SyncMode

class RestoreOptionsPresenter(private val router: RestoreOptionsModule.IRouter, private val state: RestoreOptionsModule.State)
    : RestoreOptionsModule.IViewDelegate {

    var view: RestoreOptionsModule.IView? = null

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
