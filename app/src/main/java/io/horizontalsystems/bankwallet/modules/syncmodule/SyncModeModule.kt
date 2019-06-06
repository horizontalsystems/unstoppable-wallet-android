package io.horizontalsystems.bankwallet.modules.syncmodule

import android.content.Context
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.entities.SyncMode

object SyncModeModule {

    interface IView {
        fun updateSyncMode(syncMode: SyncMode)
        fun showError(error: Int)
        fun showConfirmationDialog()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onNextClick()
        fun onFastSyncModeSelect()
        fun onSlowSyncModeSelect()
        fun didConfirm(words: List<String>)
    }

    interface IInteractor {
        fun getSyncMode(): SyncMode
        fun setSyncMode(syncMode: SyncMode)
        fun restore(words: List<String>)
    }

    interface IInteractorDelegate {
        fun didRestore()
        fun didFailToRestore(exception: Exception)
    }

    interface IRouter {
        fun navigateToSetPin()
    }

    fun start(context: Context, words: List<String>) {
        SyncModeActivity.start(context, words)
    }

    fun init(view: SyncModeViewModel, router: IRouter, keystoreSafeExecute: IKeyStoreSafeExecute) {
        val interactor = SyncModeInteractor(App.localStorage, App.authManager, App.wordsManager, keystoreSafeExecute)
        val presenter = SyncModePresenter(interactor, router, State())

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    class State{
        var syncMode: SyncMode? = null
    }
}
