package io.horizontalsystems.bankwallet.modules.backup

import android.content.Context
import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.App

object BackupModule {

    interface View

    interface ViewDelegate {
        fun onClickCancel()
        fun onClickBackup()
        fun onBackedUp(accountId: String)
    }

    interface Interactor {
        fun backup()
        fun setBackedUp(accountId: String)
    }

    interface InteractorDelegate {
        fun onPinUnlock()
    }

    interface Router {
        fun startPinModule()
        fun startBackupModule(account: Account)
        fun close()
    }

    //  helpers

    fun start(context: Context, account: Account) {
        BackupActivity.start(context, account)
    }

    fun init(view: BackupViewModel, router: Router, account: Account) {
        val interactor = BackupInteractor(router, App.lockManager, App.accountManager, App.pinManager)
        val presenter = BackupPresenter(interactor, router, account)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
