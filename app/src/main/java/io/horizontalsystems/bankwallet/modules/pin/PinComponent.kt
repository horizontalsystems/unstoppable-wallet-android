package io.horizontalsystems.bankwallet.modules.pin

import android.content.Context
import io.horizontalsystems.bankwallet.core.BackgroundManager
import io.horizontalsystems.bankwallet.core.BackgroundManagerState
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.UserManager
import io.horizontalsystems.bankwallet.modules.pin.core.AppLockManager
import io.horizontalsystems.bankwallet.modules.pin.core.PinDbStorage
import io.horizontalsystems.bankwallet.modules.pin.core.PinManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.IPinSettingsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PinComponent(
    private val context: Context,
    private val pinSettingsStorage: IPinSettingsStorage,
    private val userManager: UserManager,
    private val pinDbStorage: PinDbStorage,
    private val backgroundManager: BackgroundManager,
    private val localStorage: ILocalStorage
) : IPinComponent {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val pinManager: PinManager by lazy {
        PinManager(pinDbStorage)
    }

    private val lockManager: AppLockManager by lazy {
        AppLockManager(context, pinManager, localStorage)
    }

    init {
        observeBackgroundState()
    }

    private fun observeBackgroundState() {
        scope.launch {
            backgroundManager.stateFlow.collect { state ->
                when (state) {
                    BackgroundManagerState.EnterForeground -> {
                        willEnterForeground()
                    }
                    BackgroundManagerState.EnterBackground -> {
                        didEnterBackground()
                    }
                }
            }
        }
    }

    // Expose lock state
    override val isLockedFlow: StateFlow<Boolean>
        get() = lockManager.isLockedFlow

    override val isLocked: Boolean
        get() = lockManager.isLocked

    // PIN set observable
    override val pinSetFlow: SharedFlow<Unit>
        get() = pinManager.pinSetFlow

    override val isPinSet: Boolean
        get() = pinManager.isPinSet

    // Biometric settings
    override var isBiometricAuthEnabled: Boolean
        get() = pinSettingsStorage.biometricAuthEnabled
        set(value) {
            pinSettingsStorage.biometricAuthEnabled = value
        }

    // PIN validation
    override fun isUnique(pin: String, forDuress: Boolean): Boolean {
        val level = if (forDuress) {
            userManager.getUserLevel() + 1
        } else {
            userManager.getUserLevel()
        }
        return pinManager.isUnique(pin, level)
    }

    override fun validateCurrentLevel(pin: String): Boolean {
        val pinLevel = pinManager.getPinLevelSync(pin) ?: return false
        return pinLevel == userManager.getUserLevel()
    }

    override fun isDuressPinSet(): Boolean {
        return pinManager.isPinSetForLevel(userManager.getUserLevel() + 1)
    }

    // PIN management
    override fun setPin(pin: String) {
        scope.launch {
            if (lockManager.isLocked) {
                lockManager.unlock()
            }
            pinManager.store(pin, userManager.getUserLevel())
        }
    }

    override fun setDuressPin(pin: String) {
        scope.launch {
            pinManager.store(pin, userManager.getUserLevel() + 1)
        }
    }

    override fun disablePin() {
        scope.launch {
            pinManager.disablePin(userManager.getUserLevel())
            userManager.disallowAccountsForDuress()
        }
    }

    override fun disableDuressPin() {
        scope.launch {
            pinManager.disableDuressPin(userManager.getUserLevel() + 1)
            userManager.disallowAccountsForDuress()
        }
    }

    // Unlock operations
    override fun unlock(pin: String): Boolean {
        val pinLevel = pinManager.getPinLevelSync(pin) ?: return false

        lockManager.unlock()
        userManager.setUserLevel(pinLevel)

        return true
    }

    override fun onBiometricUnlock() {
        lockManager.unlock()
        userManager.setUserLevel(pinManager.getPinLevelLast())
    }

    override fun initDefaultPinLevel() {
        userManager.setUserLevel(pinManager.getPinLevelLast())
    }

    // Lifecycle methods
    override fun willEnterForeground() {
        lockManager.onAppForeground()
    }

    override fun didEnterBackground() {
        lockManager.onAppBackground()
    }

    override fun updateLastExitDateBeforeRestart() {
        lockManager.updateLastExitDate()
    }

    override fun keepUnlocked() {
        lockManager.setKeepUnlocked()
    }

    // Cleanup
    fun dispose() {
        scope.cancel()
    }
}