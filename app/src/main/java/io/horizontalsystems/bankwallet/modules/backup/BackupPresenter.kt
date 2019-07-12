package io.horizontalsystems.bankwallet.modules.backup

import io.horizontalsystems.bankwallet.entities.Account

class BackupPresenter(
        private val interactor: BackupModule.Interactor,
        private val router: BackupModule.Router,
        private val account: Account)
    : BackupModule.ViewDelegate, BackupModule.InteractorDelegate {

    //  View

    var view: BackupModule.View? = null

    //  Interactor delegate

    override fun onPinUnlock() {
        router.startBackupModule(account)
    }

    //  View delegates

    override fun onClickCancel() {
        router.close()
    }

    override fun onClickBackup() {
        interactor.backup()
    }

    override fun onBackedUp(accountId: String) {
        interactor.setBackedUp(accountId)
    }
}

