package io.horizontalsystems.pin

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.modules.pin.core.ILockoutUntilDateFactory
import io.horizontalsystems.bankwallet.modules.pin.core.LockoutManager
import io.horizontalsystems.bankwallet.modules.pin.core.LockoutState
import io.horizontalsystems.bankwallet.modules.pin.core.UptimeProvider
import io.horizontalsystems.core.ILockoutStorage
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import java.util.Date

class LockoutManagerTest {

    private val localStorage = Mockito.mock(ILockoutStorage::class.java)
    private val uptimeProvider = Mockito.mock(UptimeProvider::class.java)
    private val lockoutUntilDateFactory = Mockito.mock(ILockoutUntilDateFactory::class.java)
    private val lockoutManager = LockoutManager(localStorage, uptimeProvider, lockoutUntilDateFactory)

    @Test
    fun didFailUnlock_first() {
        val oldAttempts = null
        val newAttempts = 1
        whenever(localStorage.failedAttempts).thenReturn(oldAttempts)

        lockoutManager.didFailUnlock()
        verify(localStorage).failedAttempts = newAttempts
    }

    @Test
    fun didFailUnlock_third() {
        val oldAttempts = 2
        val newAttempts = 3
        whenever(localStorage.failedAttempts).thenReturn(oldAttempts)

        lockoutManager.didFailUnlock()
        verify(localStorage).failedAttempts = newAttempts
    }

//    @Test
//    fun currentStateUnlocked() {
//        val hasFailedAttempts = false
//        whenever(localStorage.failedAttempts).thenReturn(null)
//
//        val state = LockoutState.Unlocked(hasFailedAttempts)
//
//        Assert.assertEquals(lockoutManager.currentState, state)
//    }

//    @Test
//    fun currentStateUnlocked_WithTwoAttempts() {
//        val hasFailedAttempts = true
//        val failedAttempts = 2
//        whenever(localStorage.failedAttempts).thenReturn(failedAttempts)
//
//        val state = LockoutState.Unlocked(hasFailedAttempts)
//
//        Assert.assertEquals(lockoutManager.currentState, state)
//    }

    @Test
    fun currentStateUnlocked_NotLessThanOne() {
        val date = Date()
        val timestamp = date.time
        val unlockDate = Date()
        unlockDate.time = date.time + 5000

        val failedAttempts = 7

        whenever(localStorage.failedAttempts).thenReturn(failedAttempts)
        whenever(localStorage.lockoutUptime).thenReturn(timestamp)
        whenever(uptimeProvider.uptime).thenReturn(timestamp)
        whenever(lockoutUntilDateFactory.lockoutUntilDate(failedAttempts, timestamp, timestamp)).thenReturn(unlockDate)

        val state = LockoutState.Locked(unlockDate)

        Assert.assertEquals(lockoutManager.currentState, state)
    }

    @Test
    fun updateLockoutTimestamp() {
        val failedAttempts = 4
        val timestamp = Date().time

        whenever(localStorage.failedAttempts).thenReturn(failedAttempts)

        whenever(uptimeProvider.uptime).thenReturn(timestamp)

        lockoutManager.didFailUnlock()

        verify(localStorage).lockoutUptime = timestamp
    }

    @Test
    fun currentStateLocked() {
        val date = Date()
        val timestamp = date.time

        val unlockDate = Date()
        unlockDate.time = date.time + 5000

        val state = LockoutState.Locked(unlockDate)

        val failedAttempts = 5
        whenever(localStorage.failedAttempts).thenReturn(failedAttempts)
        whenever(localStorage.lockoutUptime).thenReturn(timestamp)
        whenever(uptimeProvider.uptime).thenReturn(timestamp)
        whenever(lockoutUntilDateFactory.lockoutUntilDate(failedAttempts, timestamp, timestamp)).thenReturn(unlockDate)

        Assert.assertEquals(lockoutManager.currentState, state)
    }


}
