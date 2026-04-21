package cash.p.terminal.modules.pin

import cash.p.terminal.core.App
import io.horizontalsystems.core.DispatcherProvider
import cash.p.terminal.core.managers.DefaultUserManager
import cash.p.terminal.core.tryOrNull
import cash.p.terminal.domain.usecase.ResetUseCase
import cash.p.terminal.domain.usecase.DeleteAllContactsUseCase
import cash.p.terminal.modules.pin.core.LockManager
import cash.p.terminal.modules.pin.core.PinDbStorage
import cash.p.terminal.modules.pin.core.PinLevels
import cash.p.terminal.modules.pin.core.PinManager
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.IPinSettingsStorage
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class PinComponent(
    private val pinSettingsStorage: IPinSettingsStorage,
    private val userManager: DefaultUserManager,
    private val pinDbStorage: PinDbStorage,
    private val backgroundManager: BackgroundManager,
    private val resetUseCase: ResetUseCase,
    private val deleteAllContactsUseCase: DeleteAllContactsUseCase,
    private val dispatcherProvider: DispatcherProvider,
    scope: CoroutineScope = CoroutineScope(Executors.newFixedThreadPool(5).asCoroutineDispatcher())
) : IPinComponent {

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
                    BackgroundManagerState.AllActivitiesDestroyed,
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
        LockManager(pinManager, App.localStorage, App.instance)
    }

    override val pinSetFlowable: Flowable<Unit>
        get() = pinManager.pinSetSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val isLockedFlow: StateFlow<Boolean>
        get() = appLockManager.isLocked

    override var isBiometricAuthEnabled: Boolean
        get() = pinSettingsStorage.biometricAuthEnabled
        set(value) {
            pinSettingsStorage.biometricAuthEnabled = value
        }

    override val isPinSet: Boolean
        get() = pinManager.isPinSet

    override fun getDuressLevel(): Int {
        val level = userManager.getUserLevel() + 1
        // Cap duress level to prevent reaching reserved levels
        if (level >= PinLevels.SECURE_RESET) {
            throw IllegalStateException("Cannot create duress PIN: too many duress levels")
        }
        return level
    }

    override fun isUnique(pin: String, forDuress: Boolean): Boolean {
        val level = if (forDuress) {
            tryOrNull { getDuressLevel() } ?: return false
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
        pinManager.store(pin, getDuressLevel())
    }

    override fun validateCurrentLevel(pin: String): Boolean {
        val pinLevel = pinManager.getPinLevel(pin) ?: return false
        return pinLevel == userManager.getUserLevel()
    }

    override fun isDuressPinSet(): Boolean {
        return tryOrNull { pinManager.isPinSetForLevel(getDuressLevel()) } ?: false
    }

    override fun disablePin() {
        pinManager.disablePin(userManager.getUserLevel())
        userManager.disallowAccountsForDuress()
    }

    override fun disableDuressPin() {
        tryOrNull {
            pinManager.disableDuressPin(getDuressLevel())
            userManager.disallowAccountsForDuress()
        }
    }

    /**
     * @pinLevelDetected - level detected for the entered PIN
     */
    override suspend fun unlock(pin: String, pinLevelDetected: Int?): Boolean = withContext(dispatcherProvider.io) {
        if (pinLevelDetected == PinLevels.DELETE_CONTACTS) {
            deleteAllContactsUseCase()
            return@withContext false
        }

        var pinLevel = PinLevels.resolvedUserLevelAfterUnlock(pinLevelDetected) ?: return@withContext false

        if (pinLevelDetected == PinLevels.SECURE_RESET) {
            disableSecureResetPin()
            resetUseCase()
            pinManager.store(pin, 0)
            pinLevel = 0
        }

        appLockManager.onUnlock()
        userManager.setUserLevel(pinLevel)

        true
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

    override fun getPinLevel(pin: String): Int? {
        return pinManager.getPinLevel(pin)
    }

    override fun setHiddenWalletPin(pin: String): Int {
        val nextLevel = pinManager.getNextHiddenWalletLevel()
        pinManager.store(pin, nextLevel)
        return nextLevel
    }

    override fun setSecureResetPin(pin: String) {
        pinManager.store(pin, PinLevels.SECURE_RESET)
    }

    override fun isSecureResetPinSet(): Boolean {
        return pinManager.isPinSetForLevel(PinLevels.SECURE_RESET)
    }

    override fun disableSecureResetPin() {
        pinManager.disablePin(PinLevels.SECURE_RESET)
    }

    override fun getAllPinLevels(): List<Int> {
        return pinDbStorage.getAllLevels()
    }

    override fun setDeleteContactsPin(pin: String) {
        pinManager.store(pin, PinLevels.DELETE_CONTACTS)
    }

    override fun isDeleteContactsPinSet(): Boolean {
        return pinManager.isPinSetForLevel(PinLevels.DELETE_CONTACTS)
    }

    override fun disableDeleteContactsPin() {
        pinManager.disablePin(PinLevels.DELETE_CONTACTS)
    }

    override fun setLogLoggingPin(pin: String) {
        val userLevel = userManager.getUserLevel()
        require(userLevel >= 0) { "Log logging PIN not supported for hidden wallets" }
        pinManager.store(pin, PinLevels.logLoggingLevelFor(userLevel))
    }

    override fun isLogLoggingPinSet(): Boolean {
        val userLevel = userManager.getUserLevel()
        if (userLevel < 0) return false
        return pinManager.isPinSetForLevel(PinLevels.logLoggingLevelFor(userLevel))
    }

    /**
     * Disables the log logging PIN associated with the duress level
     * to prevent having a passcode inside duress mode when duress mode removed
     */
    override fun disableLogLoggingPinForDuress() {
        pinManager.disablePin(PinLevels.logLoggingLevelFor(getDuressLevel()))
    }

    override fun disableLogLoggingPin() {
        val userLevel = userManager.getUserLevel()
        if (userLevel < 0) return
        pinManager.disablePin(PinLevels.logLoggingLevelFor(userLevel))
    }

    override fun validateLogLoggingPin(pin: String): Boolean {
        val pinLevel = pinManager.getPinLevel(pin) ?: return false
        val userLevel = userManager.getUserLevel()
        if (userLevel < 0) return false
        return pinLevel == PinLevels.logLoggingLevelFor(userLevel)
    }
}
