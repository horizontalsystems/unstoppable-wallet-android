package io.horizontalsystems.bankwallet.modules.backup

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

object BackupModule {

    interface View

    interface ViewDelegate {
        fun onClickCancel()
        fun onClickBackup()
        fun didBackup()
        fun didUnlock()
        fun didCancelUnlock()
    }

    interface Interactor {
        val isPinSet: Boolean

        fun setBackedUp(accountId: String)
        fun predefinedAccountType(accountType: AccountType): PredefinedAccountType?
    }

    interface InteractorDelegate

    interface Router {
        fun startUnlockPinModule()
        fun startBackupWordsModule(words: List<String>, accountTypeTitle: Int, additionalInfo: String?)
        fun close()
        fun showSuccessAndFinish()
    }

    //  helpers

    fun init(view: BackupViewModel, router: Router, account: Account) {
        val interactor = BackupInteractor(App.backupManager, App.pinComponent, App.predefinedAccountTypeManager)
        val presenter = BackupPresenter(interactor, router, account)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
