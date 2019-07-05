package io.horizontalsystems.bankwallet.modules.restore

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.SyncMode

object RestoreModule {

    interface View

    interface ViewDelegate {
        fun onSelect(accountType: PredefinedAccountType)
        fun didRestore(accountType: AccountType, syncMode: SyncMode)
    }

    interface Router {
        fun navigateToRestoreWords()
        fun close()
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, RestoreActivity::class.java))
    }

    fun init(view: RestoreViewModel, router: Router) {
        val presenter = RestorePresenter(router, App.accountCreator)

        view.delegate = presenter
        presenter.view = view
    }
}
