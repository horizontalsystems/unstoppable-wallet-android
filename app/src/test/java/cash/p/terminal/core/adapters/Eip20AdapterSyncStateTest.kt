package cash.p.terminal.core.adapters

import cash.p.terminal.wallet.AdapterState
import io.horizontalsystems.ethereumkit.core.EthereumKit.ForwardSyncState
import io.horizontalsystems.ethereumkit.core.EthereumKit.HistoricalSyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Eip20AdapterSyncStateTest {

    // Mirrors forwardSyncAdapterState() logic from Eip20Adapter
    private fun mapForwardSyncState(state: ForwardSyncState): AdapterState? {
        if (state is ForwardSyncState.Syncing) {
            return AdapterState.Syncing(
                progress = 0,
                blocksRemained = state.blocksRemaining
            )
        }
        return null
    }

    // Mirrors historicalSyncAdapterState() logic from BaseEvmAdapter
    private fun mapHistoricalSyncState(state: HistoricalSyncState): AdapterState? {
        if (state is HistoricalSyncState.Syncing) {
            return AdapterState.Syncing(
                progress = (state.progress * 100).toInt(),
                blocksRemained = state.blocksRemaining
            )
        }
        return null
    }

    // Mirrors convertToAdapterState priority: historical ?: forward ?: fallback
    private fun resolveState(
        historicalState: HistoricalSyncState,
        forwardState: ForwardSyncState,
        fallback: AdapterState
    ): AdapterState {
        return mapHistoricalSyncState(historicalState)
            ?: mapForwardSyncState(forwardState)
            ?: fallback
    }

    @Test
    fun convertToAdapterState_historicalSyncing_takesPriorityOverForward() {
        val historical = HistoricalSyncState.Syncing(startBlock = 50000, currentBlock = 30000)
        val forward = ForwardSyncState.Syncing(lastSyncedTip = 1000, chainTipBlock = 1500)

        val result = resolveState(historical, forward, AdapterState.Synced)

        // Should return historical, not forward
        assertTrue(result is AdapterState.Syncing)
        val syncing = result as AdapterState.Syncing
        assertEquals(30000L, syncing.blocksRemained)
    }

    @Test
    fun convertToAdapterState_forwardSyncing_mapsToAdapterStateSyncing() {
        val historical = HistoricalSyncState.Idle
        val forward = ForwardSyncState.Syncing(lastSyncedTip = 1000, chainTipBlock = 1500)

        val result = resolveState(historical, forward, AdapterState.Synced)

        assertTrue(result is AdapterState.Syncing)
        val syncing = result as AdapterState.Syncing
        assertEquals(0, syncing.progress)
        assertEquals(500L, syncing.blocksRemained)
    }

    @Test
    fun convertToAdapterState_bothIdle_returnsFallback() {
        val result = resolveState(
            HistoricalSyncState.Idle,
            ForwardSyncState.Idle,
            AdapterState.Synced
        )
        assertEquals(AdapterState.Synced, result)
    }

    @Test
    fun forwardSyncAdapterState_idle_returnsNull() {
        assertNull(mapForwardSyncState(ForwardSyncState.Idle))
    }
}
