package io.horizontalsystems.bankwallet.modules.pin

import android.app.Activity
import io.horizontalsystems.bankwallet.modules.pin.core.LockManager
import io.horizontalsystems.bankwallet.modules.pin.core.PinManager
import io.horizontalsystems.core.IEncryptionManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.IPinStorage
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable

class PinComponent(
    private val pinStorage: IPinStorage,
    private val encryptionManager: IEncryptionManager,
    private val excludedActivityNames: List<String>
) : IPinComponent {

    private val pinManager: PinManager by lazy {
        PinManager(encryptionManager, pinStorage)
    }

    private val appLockManager: LockManager by lazy {
        LockManager(pinManager)
    }

    override val pinSetFlowable: Flowable<Unit>
        get() = pinManager.pinSetSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val isLocked: Boolean
        get() = appLockManager.isLocked && isPinSet

    override var isBiometricAuthEnabled: Boolean
        get() = pinStorage.biometricAuthEnabled
        set(value) {
            pinStorage.biometricAuthEnabled = value
        }

    override val isPinSet: Boolean
        get() = pinManager.isPinSet

    override fun store(pin: String) {
        if (appLockManager.isLocked) {
            appLockManager.onUnlock()
        }

        pinManager.store(pin)
    }

    override fun validate(pin: String): Boolean {
        return pinManager.validate(pin)
    }

    override fun clear() {
        pinManager.clear()
    }

    override fun onUnlock() {
        appLockManager.onUnlock()
    }

    override fun lock() {
        appLockManager.lock()
    }

    override fun updateLastExitDateBeforeRestart() {
        appLockManager.updateLastExitDate()
    }

    override fun willEnterForeground(activity: Activity) {
        appLockManager.willEnterForeground()
    }

    override fun didEnterBackground() {
        appLockManager.didEnterBackground()
    }

    override fun shouldShowPin(activity: Activity): Boolean {
        return isLocked && !excludedActivityNames.contains(activity::class.java.name)
    }
}
