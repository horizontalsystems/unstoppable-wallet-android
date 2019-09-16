package io.horizontalsystems.bankwallet.modules.restore.options

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.bankwallet.entities.SyncMode

object RestoreOptionsModule {

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

    fun start(context: AppCompatActivity, requestCode: Int) {
        context.startActivityForResult(Intent(context, RestoreOptionsActivity::class.java), requestCode)
    }

    fun init(view: RestoreOptionsViewModel, router: IRouter) {
        val presenter = RestoreOptionsPresenter(router, State())

        view.delegate = presenter
        presenter.view = view
    }

    class State {
        var syncMode: SyncMode = SyncMode.FAST
    }
}
