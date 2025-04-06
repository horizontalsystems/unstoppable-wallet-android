package cash.p.terminal.modules.pin

import cash.p.terminal.core.App
import cash.p.terminal.core.managers.UserManager
import cash.p.terminal.modules.pin.core.LockManager
import cash.p.terminal.modules.pin.core.PinDbStorage
import cash.p.terminal.modules.pin.core.PinManager
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.IPinSettingsStorage
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PinComponent(
    private val pinSettingsStorage: IPinSettingsStorage,
    private val userManager: UserManager,
    private val pinDbStorage: PinDbStorage,
    private val backgroundManager: BackgroundManager
) : IPinComponent {

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            backgroundManager.stateFlow.collect { state ->
                when (state) {
                    BackgroundManagerState.EnterForeground -> {
                        willEnterForeground()
                    }
                    BackgroundManagerState.EnterBackground -> {
                        didEnterBackground()
                    }
                    BackgroundManagerState.AllActivitiesDestroyed -> {
                       lock()
                    }
                    BackgroundManagerState.Unknown -> {
                        //do nothing
                    }
                }
            }
        }
    }

    private val pinManager: PinManager by lazy {
        PinManager(pinDbStorage)
    }

    private val appLockManager: LockManager by lazy {
        LockManager(pinManager, App.localStorage)
    }

    override val pinSetFlowable: Flowable<Unit>
        get() = pinManager.pinSetSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val isLocked: StateFlow<Boolean> = appLockManager.isLocked
        .map { isLocked -> isLocked && isPinSet }
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO),
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    override var isBiometricAuthEnabled: Boolean
        get() = pinSettingsStorage.biometricAuthEnabled
        set(value) {
            pinSettingsStorage.biometricAuthEnabled = value
        }

    override val isPinSet: Boolean
        get() = pinManager.isPinSet

    override fun isUnique(pin: String, forDuress: Boolean): Boolean {
        val level = if (forDuress) {
            userManager.getUserLevel() + 1
        } else {
            userManager.getUserLevel()
        }
        return pinManager.isUnique(pin, level)
    }

    override fun setPin(pin: String) {
        if (appLockManager.isLocked.value) {
            appLockManager.onUnlock()
        }

        pinManager.store(pin, userManager.getUserLevel())
    }

    override fun setDuressPin(pin: String) {
        pinManager.store(pin, userManager.getUserLevel() + 1)
    }

    override fun validateCurrentLevel(pin: String): Boolean {
        val pinLevel = pinManager.getPinLevel(pin) ?: return false
        return pinLevel == userManager.getUserLevel()
    }

    override fun isDuressPinSet(): Boolean {
        return pinManager.isPinSetForLevel(userManager.getUserLevel() + 1)
    }

    override fun disablePin() {
        pinManager.disablePin(userManager.getUserLevel())
        userManager.disallowAccountsForDuress()
    }

    override fun disableDuressPin() {
        pinManager.disableDuressPin(userManager.getUserLevel() + 1)
        userManager.disallowAccountsForDuress()
    }

    override fun unlock(pin: String): Boolean {
        val pinLevel = pinManager.getPinLevel(pin) ?: return false

        appLockManager.onUnlock()
        userManager.setUserLevel(pinLevel)

        return true
    }

    override fun initDefaultPinLevel() {
        userManager.setUserLevel(pinManager.getPinLevelLast())
    }

    override fun onBiometricUnlock() {
        appLockManager.onUnlock()
        userManager.setUserLevel(pinManager.getPinLevelLast())
    }

    override fun lock() {
        appLockManager.lock()
    }

    override fun updateLastExitDateBeforeRestart() {
        appLockManager.updateLastExitDate()
    }

    override fun willEnterForeground() {
        appLockManager.willEnterForeground()
    }

    override fun didEnterBackground() {
        appLockManager.didEnterBackground()
    }

    override fun keepUnlocked() {
        appLockManager.keepUnlocked()
    }
}
