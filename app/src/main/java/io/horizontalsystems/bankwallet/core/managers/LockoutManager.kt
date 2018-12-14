package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ILockoutManager
import io.horizontalsystems.bankwallet.core.ILockoutUntilDateFactory
import io.horizontalsystems.bankwallet.core.IUptimeProvider
import io.horizontalsystems.bankwallet.entities.LockoutState
import java.util.*

class LockoutManager(
        private val localStorage: ILocalStorage,
        private val uptimeProvider: IUptimeProvider,
        private val lockoutUntilDateFactory: ILockoutUntilDateFactory) : ILockoutManager {

    private val lockoutThreshold = 5

    override val currentState: LockoutState
    get() {
        val failedAttempts = localStorage.failedAttempts
        val attemptsLeft = failedAttempts?.let {
            val attempts = lockoutThreshold - it
            if (attempts < 1) 1 else attempts
        }

        failedAttempts?.let {
            if (it >= lockoutThreshold) {
                val currentDate = Date()
                val currentTime = currentDate.time
                val lockoutTimestamp = localStorage.lockoutTimestamp ?: currentTime
                val untilDate: Date = lockoutUntilDateFactory.lockoutUntilDate(it, lockoutTimestamp, uptimeProvider.uptime)
                if (untilDate > currentDate) {
                    return LockoutState.Locked(untilDate)
                }
            }
        }

        return LockoutState.Unlocked(attemptsLeft)
    }

    override fun didFailUnlock() {
        val attempts = (localStorage.failedAttempts ?: 0) + 1
        if (attempts >= lockoutThreshold) {
            localStorage.lockoutTimestamp = uptimeProvider.uptime
        }
        localStorage.failedAttempts = attempts
    }

    override fun dropFailedAttempts() {
        localStorage.failedAttempts = null
    }

}
