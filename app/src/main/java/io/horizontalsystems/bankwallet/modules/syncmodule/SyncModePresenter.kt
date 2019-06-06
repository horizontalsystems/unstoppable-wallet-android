package io.horizontalsystems.bankwallet.modules.syncmodule

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.SyncMode

class SyncModePresenter(
        private val interactor: SyncModeModule.IInteractor,
        private val router: SyncModeModule.IRouter,
        private val state: SyncModeModule.State) : SyncModeModule.IViewDelegate, SyncModeModule.IInteractorDelegate {

    var view: SyncModeModule.IView? = null

    override fun viewDidLoad() {
        val currentSyncMode = interactor.getSyncMode()
        view?.updateSyncMode(currentSyncMode)
    }

    override fun onFastSyncModeSelect() {
        val syncMode = SyncMode.FAST
        state.syncMode = syncMode
        view?.updateSyncMode(syncMode)
    }

    override fun onSlowSyncModeSelect() {
        val syncMode = SyncMode.SLOW
        state.syncMode = syncMode
        view?.updateSyncMode(syncMode)
    }

    override fun onNextClick() {
        view?.showConfirmationDialog()
    }

    override fun didConfirm(words: List<String>) {
        val syncMode = state.syncMode ?: interactor.getSyncMode()
        interactor.restore(words, syncMode)
    }

    override fun didRestore() {
        router.navigateToSetPin()
    }

    override fun didFailToRestore(exception: Exception) {
        view?.showError(R.string.Restore_RestoreFailed)
    }

}
