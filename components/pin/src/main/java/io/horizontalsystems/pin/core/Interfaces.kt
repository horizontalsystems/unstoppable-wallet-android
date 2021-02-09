package io.horizontalsystems.pin.core

import java.util.*

interface ILockoutManager {
    fun didFailUnlock()
    fun dropFailedAttempts()

    val currentState: LockoutState
}

interface ILockoutUntilDateFactory {
    fun lockoutUntilDate(failedAttempts: Int, lockoutUptime: Long, uptime: Long): Date?
}

interface OneTimerDelegate {
    fun onFire()
}
