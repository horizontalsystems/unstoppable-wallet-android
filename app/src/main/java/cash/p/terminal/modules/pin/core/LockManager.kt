package cash.p.terminal.modules.pin.core

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.modules.settings.security.autolock.AutoLockInterval
import io.horizontalsystems.core.helpers.DateHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

class LockManager(
    private val pinManager: PinManager,
    private val localStorage: ILocalStorage
) {

    private val _isLocked = MutableStateFlow(true)
    val isLocked = _isLocked.asStateFlow()
    private var appLastVisitTime: Long = 0
    private var keepUnlocked = false

    fun didEnterBackground() {
        if (isLocked.value) {
            return
        }

        appLastVisitTime = Date().time
    }

    fun willEnterForeground() {
        if (isLocked.value || !pinManager.isPinSet) {
            return
        }

        val autoLockInterval = localStorage.autoLockInterval
        val secondsAgo = DateHelper.getSecondsAgo(appLastVisitTime)

        if (keepUnlocked) {
            keepUnlocked = false
            if (autoLockInterval == AutoLockInterval.IMMEDIATE && secondsAgo < 60) {
                return
            }
        }
        if (secondsAgo >= autoLockInterval.intervalInSeconds) {
            _isLocked.value = true
        }
    }

    fun onUnlock() {
        _isLocked.value = false
    }

    fun updateLastExitDate() {
        appLastVisitTime = Date().time
    }

    fun lock() {
        _isLocked.value = true
        appLastVisitTime = 0
    }

    fun keepUnlocked() {
        keepUnlocked = true
    }

}
