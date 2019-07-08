package io.horizontalsystems.bankwallet.modules.pin.unlock

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.AuthManager
import io.horizontalsystems.bankwallet.core.managers.OneTimeTimer
import io.horizontalsystems.bankwallet.entities.LockoutState

class UnlockPinInteractor(
        private val keystoreSafeExecute: IKeyStoreSafeExecute,
        private val localStorage: ILocalStorage,
        private val authManager: AuthManager,
        private val pinManager: IPinManager,
        private val lockManager: ILockManager,
        private val encryptionManager: IEncryptionManager,
        private val lockoutManager: ILockoutManager,
        private val timer: OneTimeTimer) : UnlockPinModule.IUnlockPinInteractor, IOneTimerDelegate {

    var delegate: UnlockPinModule.IUnlockPinInteractorDelegate? = null

    init {
        timer.delegate = this
    }

    //we cache secured data here, since its main Entry point for logged in user
    override fun cacheSecuredData() {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    if (pinManager.pin.isNullOrEmpty() && pinManager.isPinSet) {
                        pinManager.safeLoad()
                    }
                    if (authManager.authData == null) {
                        authManager.safeLoad()
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
            lockoutManager.dropFailedAttempts()
        } else {
            lockoutManager.didFailUnlock()
            updateLockoutState()
        }

        return valid
    }

    override fun onUnlock() {
        delegate?.unlock()
        lockManager.onUnlock()
    }

    override fun onFire() {
        updateLockoutState()
    }

    override fun updateLockoutState() {
        val state = lockoutManager.currentState
        when(state) {
            is LockoutState.Locked -> timer.schedule(state.until)
        }

        delegate?.updateLockoutState(state)
    }

}
