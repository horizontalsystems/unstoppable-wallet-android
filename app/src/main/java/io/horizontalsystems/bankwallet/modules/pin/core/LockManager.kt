package io.horizontalsystems.bankwallet.modules.pin.core

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.core.helpers.DateHelper
import java.util.Date

class LockManager(
    private val pinManager: PinManager,
    private val localStorage: ILocalStorage
) {

    var isLocked: Boolean = true
        private set
    private val lockTimeout = 60L
    private var appLastVisitTime: Long = 0

    fun didEnterBackground() {
        if (isLocked) {
            return
        }

        appLastVisitTime = Date().time
    }

    fun willEnterForeground() {
        if (isLocked || !pinManager.isPinSet) {
            return
        }

        val autoLockInterval = localStorage.autoLockInterval
        val secondsAgo = DateHelper.getSecondsAgo(appLastVisitTime)

        if (secondsAgo >= autoLockInterval.intervalInSeconds) {
            isLocked = true
        }
    }

    fun onUnlock() {
        isLocked = false
    }

    fun updateLastExitDate() {
        appLastVisitTime = Date().time
    }

    fun lock() {
        isLocked = true
        appLastVisitTime = 0
    }

}
