package io.horizontalsystems.bankwallet.modules.pin.unlock

import io.horizontalsystems.bankwallet.core.*

class UnlockPinInteractor(
        private val keystoreSafeExecute: IKeyStoreSafeExecute,
        private val localStorage: ILocalStorage,
        private val wordsManager: IWordsManager,
        private val pinManager: IPinManager,
        private val lockManager: ILockManager) : UnlockPinModule.IUnlockPinInteractor {

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
                        }
                    }
            )
    }

    override fun biometricUnlock() {
        if (localStorage.isBiometricOn) {
            delegate?.showFingerprintInput()
        }
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
