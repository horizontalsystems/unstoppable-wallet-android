package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.AuthManager
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.hdwalletkit.Mnemonic

class RestoreInteractor(
        private val authManager: AuthManager,
        private val wordsManager: WordsManager,
        private val localStorage: ILocalStorage,
        private val keystoreSafeExecute: IKeyStoreSafeExecute) : RestoreModule.IInteractor {

    var delegate: RestoreModule.IInteractorDelegate? = null

    override fun validate(words: List<String>) {
        try {
            wordsManager.validate(words)
            delegate?.didValidate()
        } catch (e: Mnemonic.MnemonicException) {
            delegate?.didFailToValidate(e)
        }
    }

    override fun restore(words: List<String>) {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    authManager.login(words)
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
