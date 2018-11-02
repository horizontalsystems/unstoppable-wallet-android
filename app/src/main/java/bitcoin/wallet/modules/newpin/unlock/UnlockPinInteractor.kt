package bitcoin.wallet.modules.newpin.unlock

import bitcoin.wallet.core.*

class UnlockPinInteractor(
        private val keystoreSafeExecute: IKeyStoreSafeExecute,
        private val localStorage: ILocalStorage,
        private val wordsManager: IWordsManager,
        private val adapterManager: IAdapterManager,
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
                        if (adapterManager.adapters.isEmpty()) {
                            wordsManager.safeLoad()
                            adapterManager.start()
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
