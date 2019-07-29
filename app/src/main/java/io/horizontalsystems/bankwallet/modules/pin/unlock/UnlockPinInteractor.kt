package io.horizontalsystems.bankwallet.modules.pin.unlock

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.OneTimeTimer
import io.horizontalsystems.bankwallet.entities.LockoutState

class UnlockPinInteractor(
        private val localStorage: ILocalStorage,
        private val pinManager: IPinManager,
        private val lockManager: ILockManager,
        private val lockoutManager: ILockoutManager,
        private val timer: OneTimeTimer) : UnlockPinModule.IUnlockPinInteractor, IOneTimerDelegate {

    var delegate: UnlockPinModule.IUnlockPinInteractorDelegate? = null

    init {
        timer.delegate = this
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
