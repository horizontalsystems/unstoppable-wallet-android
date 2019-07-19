package io.horizontalsystems.bankwallet.modules.backup

import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.ILockManager
import io.horizontalsystems.bankwallet.core.IPinManager
import io.reactivex.disposables.Disposable

class BackupInteractor(
        private val router: BackupModule.Router,
        private val lockManager: ILockManager,
        private val backupManager: IBackupManager,
        private val pinManager: IPinManager)
    : BackupModule.Interactor {

    var delegate: BackupModule.InteractorDelegate? = null

    private var lockStateUpdateDisposable: Disposable? = null

    override fun backup() {
        if (!pinManager.isPinSet) {
            delegate?.onPinUnlock()
            return
        }

        router.startPinModule()

        lockStateUpdateDisposable?.dispose()
        lockStateUpdateDisposable = lockManager.lockStateUpdatedSignal.subscribe {
            if (!lockManager.isLocked) {
                delegate?.onPinUnlock()
                lockStateUpdateDisposable?.dispose()
            }
        }
    }

    override fun setBackedUp(accountId: String) {
        backupManager.setIsBackedUp(accountId)
    }
}
