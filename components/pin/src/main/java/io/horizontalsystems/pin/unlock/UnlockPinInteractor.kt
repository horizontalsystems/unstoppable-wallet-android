package io.horizontalsystems.pin.unlock

import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.pin.core.ILockoutManager
import io.horizontalsystems.pin.core.LockoutState
import io.horizontalsystems.pin.core.OneTimeTimer
import io.horizontalsystems.pin.core.OneTimerDelegate

class UnlockPinInteractor(
        private val pinComponent: IPinComponent,
        private val lockoutManager: ILockoutManager,
        private val systemInfoManager: ISystemInfoManager,
        private val timer: OneTimeTimer)
    : UnlockPinModule.IInteractor, OneTimerDelegate {

    var delegate: UnlockPinModule.IInteractorDelegate? = null

    init {
        timer.delegate = this
    }

    override val isBiometricAuthEnabled: Boolean
        get() = pinComponent.isBiometricAuthEnabled

    override val isBiometricAuthSupported: Boolean
        get() = systemInfoManager.biometricAuthSupported

    override fun unlock(pin: String): Boolean {
        val valid = pinComponent.validate(pin)
        if (valid) {
            pinComponent.onUnlock()
            lockoutManager.dropFailedAttempts()
        } else {
            lockoutManager.didFailUnlock()
            updateLockoutState()
        }

        return valid
    }

    override fun onUnlock() {
        delegate?.unlock()
        pinComponent.onUnlock()
    }

    override fun onFire() {
        updateLockoutState()
    }

    override fun updateLockoutState() {
        val state = lockoutManager.currentState
        when (state) {
            is LockoutState.Locked -> timer.schedule(state.until)
        }

        delegate?.updateLockoutState(state)
    }

}
