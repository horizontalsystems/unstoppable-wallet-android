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
        fun backupAccount(account: Account)
        fun unlinkAccount(id: String)
        fun onClear()
    }

    interface Interactor {
        fun loadAccounts()
        fun backupAccount(account: Account)
        fun deleteAccount(id: String)
        fun clear()
    }

    interface InteractorDelegate {
        fun didLoad(accounts: List<Account>)
        fun accessIsRestricted()
        fun openBackupWallet(account: Account)
    }

    interface Router {
        fun showPinUnlock()
        fun showBackupWallet(account: Account)
        fun close()
    }

    fun init(view: ManageKeysViewModel, router: Router) {
        val interactor = ManageKeysInteractor(App.accountManager, App.lockManager)
        val presenter = ManageKeysPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, ManageKeysActivity::class.java))
    }
}
