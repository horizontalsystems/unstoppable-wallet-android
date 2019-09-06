package io.horizontalsystems.bankwallet.modules.pin.unlock

import androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.OneTimeTimer
import io.horizontalsystems.bankwallet.entities.LockoutState

class UnlockPinInteractor(
        private val pinManager: IPinManager,
        private val lockManager: ILockManager,
        private val lockoutManager: ILockoutManager,
        private val encryptionManager: IEncryptionManager,
        private val systemInfoManager: ISystemInfoManager,
        private val timer: OneTimeTimer) : UnlockPinModule.IUnlockPinInteractor, IOneTimerDelegate {

    var delegate: UnlockPinModule.IUnlockPinInteractorDelegate? = null

    init {
        timer.delegate = this
    }

    override val isFingerprintEnabled: Boolean
        get() = pinManager.isFingerprintEnabled

    override val hasEnrolledFingerprints: Boolean
        get() = systemInfoManager.hasEnrolledFingerprints

    override val cryptoObject: CryptoObject?
        get() = encryptionManager.getCryptoObject()

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
        when (state) {
            is LockoutState.Locked -> timer.schedule(state.until)
        }

        delegate?.updateLockoutState(state)
    }

}
