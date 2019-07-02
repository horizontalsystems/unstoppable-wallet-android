package io.horizontalsystems.bankwallet.modules.settings.managekeys

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.App

object ManageKeysModule {

    interface View {
        fun show(items: List<Account>)
    }

    interface ViewDelegate {
        val items: List<Account>

        fun viewDidLoad()
        fun unlinkAccount(id: String)
        fun onClear()
    }

    interface Interactor {
        fun loadAccounts()
        fun deleteAccount(id: String)
        fun clear()
    }

    interface InteractorDelegate {
        fun didLoad(accounts: List<Account>)
    }

    interface Router {
        fun close()
    }

    fun init(view: ManageKeysViewModel, router: Router) {
        val interactor = ManageKeysInteractor(App.accountManager)
        val presenter = ManageKeysPresenter(interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, ManageKeysActivity::class.java))
    }
}
