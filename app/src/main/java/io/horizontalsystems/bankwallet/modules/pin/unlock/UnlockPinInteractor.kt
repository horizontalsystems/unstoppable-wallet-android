package io.horizontalsystems.bankwallet.modules.pin.unlock

import io.horizontalsystems.bankwallet.core.*

class UnlockPinInteractor(
        private val keystoreSafeExecute: IKeyStoreSafeExecute,
        private val localStorage: ILocalStorage,
        private val wordsManager: IWordsManager,
        private val pinManager: IPinManager,
        private val lockManager: ILockManager,
        private val encryptionManager: IEncryptionManager) : UnlockPinModule.IUnlockPinInteractor {

    var delegate: UnlockPinModule.IUnlockPinInteractorDelegate? = null

    //we cache secured data here, since its main Entry point for logged in user
    override fun cacheSecuredData() {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    if (pinManager.pin.isNullOrEmpty() && pinManager.isPinSet) {
                        pinManager.safeLoad()
                    }
                    if (wordsManager.words == null || wordsManager.words?.isEmpty() == true) {
                        wordsManager.safeLoad()
                        wordsManager.loggedInSubject.onNext(LogInState.RESUME)
                    }
                    if (isBiometricOn()) {
                        encryptionManager.getCryptoObject()?.let { delegate?.setCryptoObject(it) }
                    }
                }
        )
    }

    override fun isBiometricOn(): Boolean {
        return localStorage.isBiometricOn
    }

    override fun unlock(pin: String): Boolean {
        val valid = pinManager.validate(pin)
        if (valid) {
            lockManager.onUnlock()
        }
        return valid
    }

    override fun onUnlock() {
        delegate?.unlock()
        lockManager.onUnlock()
    }

}
