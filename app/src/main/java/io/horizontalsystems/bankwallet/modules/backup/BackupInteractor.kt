package io.horizontalsystems.bankwallet.modules.backup

import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.core.IPinManager

class BackupInteractor(private val backupManager: IBackupManager, private val pinManager: IPinManager)
    : BackupModule.Interactor {

    var delegate: BackupModule.InteractorDelegate? = null

    override val isPinSet: Boolean
        get() = pinManager.isPinSet

    override fun setBackedUp(accountId: String) {
        backupManager.setIsBackedUp(accountId)
    }
}
