package io.horizontalsystems.bankwallet.modules.restore

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.entities.SyncMode

object RestoreModule {

    interface View {
        fun reload(items: List<IPredefinedAccountType>)
    }

    interface ViewDelegate {
        val items: List<IPredefinedAccountType>

        fun viewDidLoad()
        fun onSelect(accountType: IPredefinedAccountType)
        fun didRestore(accountType: AccountType, syncMode: SyncMode)
    }

    interface Router {
        fun startRestoreWordsModule()
        fun close()
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, RestoreActivity::class.java))
    }

    fun init(view: RestoreViewModel, router: Router) {
        val presenter = RestorePresenter(router, App.accountCreator, App.predefinedAccountTypeManager)

        view.delegate = presenter
        presenter.view = view
    }
}
