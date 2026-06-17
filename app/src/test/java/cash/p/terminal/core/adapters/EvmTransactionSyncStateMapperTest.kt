package cash.p.terminal.core.adapters

import cash.p.terminal.data.repository.EvmTransactionRepository
import cash.p.terminal.wallet.AdapterState
import io.horizontalsystems.ethereumkit.core.EthereumKit.ForwardSyncState
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncError
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvmTransactionSyncStateMapperTest {
    private val repository: EvmTransactionRepository = mockk(relaxed = true)

    @Test
    fun transactionHistoryAdapterState_txSyncingFromBackgroundPoll_returnsSynced() {
        mockState(txSyncState = SyncState.Syncing())

        assertEquals(AdapterState.Synced, repository.transactionHistoryAdapterState)
    }

    @Test
    fun transactionHistoryAdapterState_forwardSyncing_returnsSyncing() {
        mockState(
            forwardSyncState = ForwardSyncState.Syncing(
                lastSyncedTip = 83_000_000,
                chainTipBlock = 83_000_500
            )
        )

        val state = repository.transactionHistoryAdapterState

        assertTrue(state is AdapterState.Syncing)
        assertEquals(500L, (state as AdapterState.Syncing).blocksRemained)
    }

    @Test
    fun transactionHistoryAdapterState_txNotStarted_returnsSynced() {
        mockState(txSyncState = SyncState.NotSynced(SyncError.NotStarted()))

        assertEquals(AdapterState.Synced, repository.transactionHistoryAdapterState)
    }

    private fun mockState(
        txSyncState: SyncState = SyncState.Synced(),
        forwardSyncState: ForwardSyncState = ForwardSyncState.Idle
    ) {
        every { repository.transactionsSyncState } returns txSyncState
        every { repository.forwardSyncState } returns MutableStateFlow(forwardSyncState)
    }
}
