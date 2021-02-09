package io.horizontalsystems.bankwallet.modules.backup

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType

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
            startBackup()
        }
    }

    override fun didBackup() {
        interactor.setBackedUp(account.id)
        router.showSuccessAndFinish()
    }

    override fun didUnlock() {
        startBackup()
    }

    override fun didCancelUnlock() {

    }

    private fun startBackup() {
        when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                startBackupWords(accountType, accountType.words)
            }
            is AccountType.Zcash -> {
                startBackupWords(accountType, accountType.words, accountType.birthdayHeight?.toString())
            }
        }
    }

    private fun startBackupWords(accountType: AccountType, words: List<String>, additionalInfo: String? = null) {
        interactor.predefinedAccountType(accountType)?.let {
            router.startBackupWordsModule(words, it.title, additionalInfo)
        }
    }
}

