package io.horizontalsystems.bankwallet.modules.backup

import io.horizontalsystems.bankwallet.entities.Account

class BackupPresenter(
        private val interactor: BackupModule.Interactor,
        private val router: BackupModule.Router,
        private val account: Account)
    : BackupModule.ViewDelegate, BackupModule.InteractorDelegate {

    //  View

    var view: BackupModule.View? = null

    //  View delegates

    override fun onClickCancel() {
        router.close()
    }

    override fun onClickBackup() {
        if (interactor.isPinSet) {
            router.startUnlockPinModule()
        } else {
            router.startBackupModule(account)
        }
    }

    override fun didBackup() {
        interactor.setBackedUp(account.id)
    }

    override fun didUnlock() {
        router.startBackupModule(account)
    }

    override fun didCancelUnlock() {

    }
}

