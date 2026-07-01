package io.horizontalsystems.bankwallet.modules.pin.core

import android.content.Context
import androidx.core.content.edit
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.modules.settings.security.autolock.AutoLockInterval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockManager(
    context: Context,
    private val pinManager: PinManager,
    private val localStorage: ILocalStorage
) {
    companion object {
        private const val PREFS_NAME = "app_lock_prefs"
        private const val KEY_LAST_BACKGROUND_TIME = "last_background_time"
        private const val KEY_KEEP_UNLOCKED = "keep_unlocked"
        private const val IMMEDIATE_LOCK_GRACE_PERIOD = 60_000L
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _isLockedFlow = MutableStateFlow(pinManager.isPinSet)
    val isLockedFlow: StateFlow<Boolean> = _isLockedFlow.asStateFlow()

    val isLocked: Boolean
        get() = _isLockedFlow.value && pinManager.isPinSet

    var keepUnlocked: Boolean
        get() = prefs.getBoolean(KEY_KEEP_UNLOCKED, false)
        private set(value) {
            prefs.edit { putBoolean(KEY_KEEP_UNLOCKED, value) }
        }

    fun onAppBackground() {
        if (_isLockedFlow.value) {
            return
        }

        val currentTime = System.currentTimeMillis()
        prefs.edit { putLong(KEY_LAST_BACKGROUND_TIME, currentTime) }
    }

    fun onAppForeground() {
        if (_isLockedFlow.value || !pinManager.isPinSet) {
            return
        }

        val lastBackgroundTime = prefs.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        if (lastBackgroundTime == 0L) {
            return
        }

        val timeInBackground = System.currentTimeMillis() - lastBackgroundTime
        val autoLockInterval = localStorage.autoLockInterval

        // Handle keep unlocked flag
        if (keepUnlocked) {
            keepUnlocked = false

            // For immediate mode, allow grace period if less than 1 minute
            if (autoLockInterval == AutoLockInterval.IMMEDIATE &&
                timeInBackground < IMMEDIATE_LOCK_GRACE_PERIOD
            ) {
                return
            }
        }

        // Check if should lock based on interval
        val intervalMillis = autoLockInterval.intervalInSeconds * 1000L
        if (timeInBackground >= intervalMillis) {
            lock()
        }
    }

    fun lock() {
        if (!pinManager.isPinSet) {
            return
        }
        _isLockedFlow.value = true
        prefs.edit { remove(KEY_LAST_BACKGROUND_TIME) }
    }

    fun unlock() {
        _isLockedFlow.value = false
    }

    fun setKeepUnlocked() {
        keepUnlocked = true
    }

    fun updateLastExitDate() {
        val currentTime = System.currentTimeMillis()
        prefs.edit { putLong(KEY_LAST_BACKGROUND_TIME, currentTime) }
    }
}