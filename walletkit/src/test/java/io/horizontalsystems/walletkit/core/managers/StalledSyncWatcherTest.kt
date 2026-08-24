package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StalledSyncWatcherTest {

    private val zcash = BlockchainType.Zcash
    private val monero = BlockchainType.Monero

    private fun watcher() = StalledSyncWatcher(CoroutineScope(Dispatchers.Unconfined))

    /** Ticks [count] stalled checks and returns the 1-based tick numbers that triggered recovery. */
    private fun StalledSyncWatcher.stalledTicks(type: BlockchainType, count: Int): List<Int> =
        (1..count).filter { shouldAct(type, stalled = true) }

    @Test
    fun `a healthy chain never acts`() {
        val watcher = watcher()
        repeat(50) {
            assertFalse(watcher.shouldAct(zcash, stalled = false))
        }
    }

    @Test
    fun `a single stalled check is not enough`() {
        val watcher = watcher()
        assertFalse(watcher.shouldAct(zcash, stalled = true))
    }

    @Test
    fun `acts on the second consecutive stalled check`() {
        val watcher = watcher()
        assertFalse(watcher.shouldAct(zcash, stalled = true))
        assertTrue(watcher.shouldAct(zcash, stalled = true))
    }

    @Test
    fun `a recovery in between resets the count`() {
        val watcher = watcher()
        assertFalse(watcher.shouldAct(zcash, stalled = true))
        assertFalse(watcher.shouldAct(zcash, stalled = false))
        // back to square one rather than acting immediately
        assertFalse(watcher.shouldAct(zcash, stalled = true))
        assertTrue(watcher.shouldAct(zcash, stalled = true))
    }

    @Test
    fun `retries back off by doubling`() {
        // 2 strikes, then 4, 8, 16, 32 -> ticks 2, 6, 14, 30, 62
        val acted = watcher().stalledTicks(zcash, count = 62)
        assertEquals(listOf(2, 6, 14, 30, 62), acted)
    }

    @Test
    fun `backoff is capped so retries never stop entirely`() {
        val watcher = watcher()
        watcher.stalledTicks(zcash, count = 62)
        // past the cap the gap stays at 32 ticks rather than growing without bound
        val laterGaps = watcher.stalledTicks(zcash, count = 96)
        assertEquals(listOf(32, 64, 96), laterGaps)
    }

    @Test
    fun `recovering resets the backoff too`() {
        val watcher = watcher()
        watcher.stalledTicks(zcash, count = 62)   // deep into the backoff
        watcher.shouldAct(zcash, stalled = false) // synced again

        // next stall is treated as fresh, not as a continuation of the old backoff
        assertFalse(watcher.shouldAct(zcash, stalled = true))
        assertTrue(watcher.shouldAct(zcash, stalled = true))
    }

    @Test
    fun `chains are tracked independently`() {
        val watcher = watcher()
        assertFalse(watcher.shouldAct(zcash, stalled = true))
        // monero stalling must not borrow zcash's strike
        assertFalse(watcher.shouldAct(monero, stalled = true))
        assertTrue(watcher.shouldAct(zcash, stalled = true))
        assertTrue(watcher.shouldAct(monero, stalled = true))
    }
}
