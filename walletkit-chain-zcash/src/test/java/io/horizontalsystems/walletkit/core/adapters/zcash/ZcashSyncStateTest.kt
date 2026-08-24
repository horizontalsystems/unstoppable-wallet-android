package io.horizontalsystems.walletkit.core.adapters.zcash

import cash.z.ecc.android.sdk.Synchronizer
import io.horizontalsystems.walletkit.core.AdapterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZcashSyncStateTest {

    @Test
    fun `progress does not resurrect a terminal STOPPED state`() {
        // STOPPED never re-emits, so letting progress overwrite it would permanently hide the
        // stall from hasUnsyncedWallet and the recovery triggers.
        val stopped = ZcashSyncState.fromStatus(Synchronizer.Status.STOPPED, lastError = null)
        assertTrue(stopped is AdapterState.NotSynced)

        assertNull(ZcashSyncState.fromProgress(stopped, progressPercent = 50, blocksRemaining = 100L))
    }

    @Test
    fun `progress does not resurrect DISCONNECTED either`() {
        val disconnected = ZcashSyncState.fromStatus(Synchronizer.Status.DISCONNECTED, lastError = null)
        assertNull(ZcashSyncState.fromProgress(disconnected, progressPercent = 10, blocksRemaining = 5L))
    }

    @Test
    fun `progress refines an ongoing sync`() {
        val refined = ZcashSyncState.fromProgress(AdapterState.Syncing(), progressPercent = 42, blocksRemaining = 7L)
        assertEquals(AdapterState.Syncing(progress = 42, blocksRemained = 7L), refined)
    }

    @Test
    fun `progress without a block estimate is ignored`() {
        assertNull(ZcashSyncState.fromProgress(AdapterState.Syncing(), progressPercent = 42, blocksRemaining = null))
    }

    @Test
    fun `progress does not restart a synced wallet`() {
        // a new sync round enters through Status.SYNCING, never through a progress emission
        assertNull(ZcashSyncState.fromProgress(AdapterState.Synced, progressPercent = 1, blocksRemaining = 3L))
    }

    @Test
    fun `stopped carries the last processor error when present`() {
        val cause = Exception("download failed")
        val state = ZcashSyncState.fromStatus(Synchronizer.Status.STOPPED, cause)
        assertEquals(cause, (state as AdapterState.NotSynced).error)
    }
}
