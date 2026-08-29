package io.horizontalsystems.walletkit.modules.pin.core

import io.horizontalsystems.walletkit.modules.main.MainModule.MainNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockGateTest {

    private val isLocked = MutableStateFlow(true)
    private val marketsTabEnabled = MutableStateFlow(true)

    // Unconfined: flow collectors run synchronously on every value change, so the gate's
    // derived state can be asserted right after mutating the inputs.
    private val gate = LockGate(isLocked, marketsTabEnabled, CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `no keypad when app is not locked`() {
        isLocked.value = false
        gate.selectedTab = MainNavigation.Balance
        gate.currentPageAccessibleWhileLocked = false

        assertFalse(gate.showUnlock)
        assertFalse(gate.isRestricted)
    }

    @Test
    fun `market tab is browsable while locked`() {
        gate.selectedTab = MainNavigation.Market
        gate.currentPageAccessibleWhileLocked = true

        assertFalse(gate.showUnlock)
        assertTrue(gate.isRestricted)
    }

    @Test
    fun `keypad shown before main screen reports a tab only if page is private`() {
        // selectedTab is null on cold start until MainViewModel reports it
        gate.currentPageAccessibleWhileLocked = true
        assertFalse(gate.showUnlock)

        gate.currentPageAccessibleWhileLocked = false
        assertTrue(gate.showUnlock)
    }

    @Test
    fun `wallet tabs require unlock`() {
        gate.currentPageAccessibleWhileLocked = true

        for (tab in listOf(MainNavigation.Balance, MainNavigation.Swap, MainNavigation.Settings)) {
            gate.selectedTab = tab
            assertTrue("keypad expected for $tab", gate.showUnlock)
            assertFalse(gate.isTabAccessibleWhileLocked(tab))
        }
        assertTrue(gate.isTabAccessibleWhileLocked(MainNavigation.Market))
    }

    @Test
    fun `private page on top requires unlock even on market tab`() {
        gate.selectedTab = MainNavigation.Market
        gate.currentPageAccessibleWhileLocked = false

        assertTrue(gate.showUnlock)
    }

    @Test
    fun `full lock when market tab is disabled`() {
        marketsTabEnabled.value = false
        gate.selectedTab = MainNavigation.Market
        gate.currentPageAccessibleWhileLocked = true

        assertTrue(gate.showUnlock)
        assertFalse(gate.isRestricted)
    }

    @Test
    fun `disabling market tab while browsing it locks the screen`() {
        gate.selectedTab = MainNavigation.Market
        assertFalse(gate.showUnlock)

        marketsTabEnabled.value = false
        assertTrue(gate.showUnlock)

        marketsTabEnabled.value = true
        assertFalse(gate.showUnlock)
    }

    @Test
    fun `requireUnlocked runs action immediately when unlocked`() {
        isLocked.value = false
        var runs = 0

        gate.requireUnlocked { runs++ }

        assertEquals(1, runs)
        assertFalse(gate.unlockRequested)
        assertFalse(gate.showUnlock)
    }

    @Test
    fun `requireUnlocked shows keypad and replays action after unlock`() {
        gate.selectedTab = MainNavigation.Market
        var runs = 0

        gate.requireUnlocked { runs++ }

        assertEquals(0, runs)
        assertTrue(gate.unlockRequested)
        assertTrue(gate.showUnlock)

        isLocked.value = false

        assertEquals(1, runs)
        assertFalse(gate.unlockRequested)
        assertFalse(gate.showUnlock)
    }

    @Test
    fun `pending action is dropped when the request is cancelled`() {
        gate.selectedTab = MainNavigation.Market
        var runs = 0

        gate.requireUnlocked { runs++ }
        gate.cancelUnlockRequest()

        assertFalse(gate.unlockRequested)
        assertFalse(gate.showUnlock)

        isLocked.value = false
        assertEquals(0, runs)
    }

    @Test
    fun `newer request replaces a pending one`() {
        gate.selectedTab = MainNavigation.Market
        val runs = mutableListOf<String>()

        gate.requireUnlocked { runs.add("first") }
        gate.requireUnlocked { runs.add("second") }
        isLocked.value = false

        assertEquals(listOf("second"), runs)
    }

    @Test
    fun `pending action runs only once`() {
        gate.selectedTab = MainNavigation.Market
        var runs = 0

        gate.requireUnlocked { runs++ }
        isLocked.value = false
        isLocked.value = true
        isLocked.value = false

        assertEquals(1, runs)
    }

    @Test
    fun `keypad stays when unlock request is cancelled on a private screen`() {
        gate.selectedTab = MainNavigation.Balance

        gate.requireUnlocked { }
        gate.cancelUnlockRequest()

        assertTrue(gate.showUnlock)
    }

    @Test
    fun `locking on a public screen notifies listeners`() {
        isLocked.value = false
        gate.selectedTab = MainNavigation.Balance
        gate.currentPageAccessibleWhileLocked = true
        var notified = 0
        gate.addRestrictedLockListener { notified++ }

        isLocked.value = true

        assertEquals(1, notified)
    }

    @Test
    fun `locking on a private page does not notify listeners`() {
        isLocked.value = false
        gate.currentPageAccessibleWhileLocked = false
        var notified = 0
        gate.addRestrictedLockListener { notified++ }

        isLocked.value = true

        assertEquals(0, notified)
        assertTrue(gate.showUnlock)
    }

    @Test
    fun `locking with market tab disabled does not notify listeners`() {
        isLocked.value = false
        marketsTabEnabled.value = false
        gate.currentPageAccessibleWhileLocked = true
        var notified = 0
        gate.addRestrictedLockListener { notified++ }

        isLocked.value = true

        assertEquals(0, notified)
        assertTrue(gate.showUnlock)
    }

    @Test
    fun `initial locked state does not notify listeners`() {
        var notified = 0
        gate.addRestrictedLockListener { notified++ }
        gate.currentPageAccessibleWhileLocked = true

        assertEquals(0, notified)
    }

    @Test
    fun `removed listener is not notified`() {
        isLocked.value = false
        var notified = 0
        val listener: () -> Unit = { notified++ }
        gate.addRestrictedLockListener(listener)
        gate.removeRestrictedLockListener(listener)

        isLocked.value = true

        assertEquals(0, notified)
    }

    @Test
    fun `listener switching tab to market keeps keypad hidden`() {
        isLocked.value = false
        gate.selectedTab = MainNavigation.Balance
        gate.addRestrictedLockListener { gate.selectedTab = MainNavigation.Market }

        isLocked.value = true

        assertFalse(gate.showUnlock)
    }
}
