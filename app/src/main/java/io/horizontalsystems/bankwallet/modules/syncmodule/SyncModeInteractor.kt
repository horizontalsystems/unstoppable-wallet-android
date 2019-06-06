package io.horizontalsystems.bankwallet.modules.syncmodule

import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.AuthManager
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.modules.restore.RestoreModule

class SyncModeInteractor(
        private val localStorage: ILocalStorage,
        private val authManager: AuthManager,
        private val wordsManager: WordsManager,
        private val keystoreSafeExecute: IKeyStoreSafeExecute) : SyncModeModule.IInteractor {

    var delegate: SyncModeModule.IInteractorDelegate? = null

    override fun getSyncMode(): SyncMode {
        return localStorage.syncMode
    }

    override fun restore(words: List<String>, syncMode: SyncMode) {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    authManager.login(words, syncMode)
                },
                onSuccess = Runnable {
                    wordsManager.isBackedUp = true
                    localStorage.iUnderstand = true
                    delegate?.didRestore()
                },
                onFailure = Runnable {
                    delegate?.didFailToRestore(RestoreModule.RestoreFailedException())
                }
        )
    }
}
