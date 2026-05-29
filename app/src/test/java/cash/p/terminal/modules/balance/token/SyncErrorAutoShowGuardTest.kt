package cash.p.terminal.modules.balance.token

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncErrorAutoShowGuardTest {

    @Test
    fun shouldAutoShowSyncError_failedAndUnlocked_returnsTrue() {
        assertTrue(shouldAutoShowSyncError(failedIconVisible = true, appLocked = false))
    }

    @Test
    fun shouldAutoShowSyncError_failedAndLocked_returnsFalse() {
        assertFalse(shouldAutoShowSyncError(failedIconVisible = true, appLocked = true))
    }

    @Test
    fun shouldAutoShowSyncError_notFailedAndUnlocked_returnsFalse() {
        assertFalse(shouldAutoShowSyncError(failedIconVisible = false, appLocked = false))
    }
}
