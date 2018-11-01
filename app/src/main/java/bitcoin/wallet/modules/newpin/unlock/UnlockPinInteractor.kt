package bitcoin.wallet.modules.newpin.unlock

import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.ILockManager
import bitcoin.wallet.core.IPinManager

class UnlockPinInteractor(
        private val keystoreSafeExecute: IKeyStoreSafeExecute,
        private val localStorage: ILocalStorage,
        private val pinManager: IPinManager,
        private val lockManager: ILockManager) : UnlockPinModule.IUnlockPinInteractor {

    var delegate: UnlockPinModule.IUnlockPinInteractorDelegate? = null

    override fun biometricUnlock() {
        if (localStorage.isBiometricOn) {
            delegate?.showFingerprintInput()
        }
    }

    override fun unlock(pin: String) {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    if (pinManager.validate(pin)) {
                        onUnlock()
                    } else {
                        delegate?.wrongPinSubmitted()
                    }
                }
        )
    }

    override fun onUnlock() {
        delegate?.unlock()
        lockManager.onUnlock()
    }

}
