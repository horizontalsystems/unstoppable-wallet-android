package io.horizontalsystems.walletkit.modules.balance.token

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// The sync error sheet opens by itself once, and only for an error that is still there
class SyncErrorAlertTest {

    @Test
    fun askedForTheFirstTimeTheSyncFails() {
        assertEquals(true, syncErrorAlert(alert = null, syncFailed = true))
    }

    @Test
    fun nothingIsAskedForWhileTheSyncIsFine() {
        assertNull(syncErrorAlert(alert = null, syncFailed = false))
    }

    @Test
    fun keptWhileTheSyncIsStillFailed() {
        assertEquals(true, syncErrorAlert(alert = true, syncFailed = true))
    }

    @Test
    fun droppedWhenTheErrorClearsBeforeItWasShown() {
        assertNull(syncErrorAlert(alert = true, syncFailed = false))
    }

    @Test
    fun askedForAgainAfterAnErrorThatWasNeverShown() {
        val dropped = syncErrorAlert(alert = true, syncFailed = false)
        assertEquals(true, syncErrorAlert(alert = dropped, syncFailed = true))
    }

    @Test
    fun notAskedForASecondTimeOnceShown() {
        assertEquals(false, syncErrorAlert(alert = false, syncFailed = true))
        assertEquals(false, syncErrorAlert(alert = false, syncFailed = false))
    }
}
