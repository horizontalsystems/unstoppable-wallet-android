package cash.p.terminal.modules.pin.core

import android.content.Context
import androidx.core.content.edit
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.ui_compose.ScreenSecurityState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LockManager(
    private val pinManager: PinManager,
    private val localStorage: ILocalStorage,
    context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isLocked = MutableStateFlow(pinManager.isPinSet)
    val isLocked: StateFlow<Boolean> = _isLocked

    init {
        ScreenSecurityState.isAppLocked = _isLocked.value
    }

    private fun setLocked(locked: Boolean) {
        _isLocked.value = locked
        ScreenSecurityState.isAppLocked = locked
    }

    private var appLastVisitTime: Long
        get() = prefs.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_BACKGROUND_TIME, value) }

    private var keepUnlocked: Boolean
        get() = prefs.getBoolean(KEY_KEEP_UNLOCKED, false)
        set(value) = prefs.edit { putBoolean(KEY_KEEP_UNLOCKED, value) }

    fun didEnterBackground() {
        if (isLocked.value) return
        appLastVisitTime = System.currentTimeMillis()
    }

    fun willEnterForeground() {
        if (isLocked.value || !pinManager.isPinSet) return

        val lockTimeoutMillis = activeLockTimeoutMillis()
        val elapsedMillis = System.currentTimeMillis() - appLastVisitTime

        if (keepUnlocked) {
            keepUnlocked = false
            if (lockTimeoutMillis == 0L && elapsedMillis < GRACE_PERIOD_MS) {
                return
            }
        }
        if (elapsedMillis >= lockTimeoutMillis) {
            setLocked(true)
        }
    }

    private fun activeLockTimeoutMillis(): Long = if (localStorage.isCalculatorModeEnabled) {
        localStorage.calculatorAutoLockOption.refillIntervalMillis
    } else {
        localStorage.autoLockInterval.intervalInSeconds * 1000L
    }

    fun onUnlock() {
        setLocked(false)
    }

    fun updateLastExitDate() {
        appLastVisitTime = System.currentTimeMillis()
    }

    fun lock() {
        setLocked(true)
        prefs.edit { remove(KEY_LAST_BACKGROUND_TIME) }
    }

    fun keepUnlocked() {
        keepUnlocked = true
    }

    companion object {
        private const val PREFS_NAME = "lock_manager_prefs"
        private const val KEY_LAST_BACKGROUND_TIME = "last_background_time"
        private const val KEY_KEEP_UNLOCKED = "keep_unlocked"
        private const val GRACE_PERIOD_MS = 60_000L
    }
}
