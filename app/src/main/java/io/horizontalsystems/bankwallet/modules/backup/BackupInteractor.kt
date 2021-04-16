package io.horizontalsystems.bankwallet.modules.backup

import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.core.IPinComponent

class BackupInteractor(
        private val backupManager: IBackupManager,
        private val pinComponent: IPinComponent
)
    : BackupModule.Interactor {

    var delegate: BackupModule.InteractorDelegate? = null

    override val isPinSet: Boolean
        get() = pinComponent.isPinSet

    override fun setBackedUp(accountId: String) {
        backupManager.setIsBackedUp(accountId)
    }

//    override fun predefinedAccountType(accountType: AccountType): PredefinedAccountType? {
//        return predefinedAccountTypeManager.predefinedAccountType(accountType)
//    }
}
