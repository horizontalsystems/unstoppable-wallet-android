package io.horizontalsystems.bankwallet.modules.syncmodule

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.bankwallet.entities.SyncMode

object SyncModeModule {

    interface IView {
        fun update(syncMode: SyncMode)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onSyncModeSelect(isFast: Boolean)
        fun didConfirm()
    }

    interface IInteractor

    interface IRouter {
        fun notifyOnSelect(syncMode: SyncMode)
    }

    fun startForResult(context: AppCompatActivity, requestCode: Int) {
        context.startActivityForResult(Intent(context, SyncModeActivity::class.java), requestCode)
    }

    fun init(view: SyncModeViewModel, router: IRouter) {
        val presenter = SyncModePresenter(router, State())

        view.delegate = presenter
        presenter.view = view
    }

    class State {
        var syncMode: SyncMode = SyncMode.FAST
    }
}
