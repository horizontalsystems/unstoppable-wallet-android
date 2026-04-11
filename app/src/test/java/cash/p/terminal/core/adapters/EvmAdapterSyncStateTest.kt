package cash.p.terminal.core.adapters

import cash.p.terminal.core.ICoinManager
import cash.p.terminal.data.repository.EvmTransactionRepository
import cash.p.terminal.wallet.AdapterState
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.ethereumkit.core.EthereumKit.ForwardSyncState
import io.horizontalsystems.ethereumkit.core.EthereumKit.HistoricalSyncState
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncError
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests production EvmAdapter.balanceState to ensure native coin sync state
 * is independent of ERC20 historical/forward discovery sync.
 */
class EvmAdapterSyncStateTest {

    private val repository: EvmTransactionRepository = mockk(relaxed = true)
    private val coinManager: ICoinManager = mockk(relaxed = true)

    private fun createAdapter(
        syncState: SyncState = SyncState.Synced(),
        txSyncState: SyncState = SyncState.Synced(),
        historicalSyncState: HistoricalSyncState = HistoricalSyncState.Idle,
        forwardSyncState: ForwardSyncState = ForwardSyncState.Idle,
        blockchainType: BlockchainType = BlockchainType.BinanceSmartChain,
    ): EvmAdapter {
        every { repository.syncState } returns syncState
        every { repository.transactionsSyncState } returns txSyncState
        every { repository.historicalSyncState } returns MutableStateFlow(historicalSyncState)
        every { repository.forwardSyncState } returns MutableStateFlow(forwardSyncState)
        every { repository.getBlockchainType() } returns blockchainType
        return EvmAdapter(repository, coinManager)
    }

    // --- Regression: native adapter must ignore ERC20 discovery sync ---

    @Test
    fun balanceState_nativeSyncedWhileHistoricalScanning83MBlocks_returnsSynced() {
        val adapter = createAdapter(
            historicalSyncState = HistoricalSyncState.Syncing(
                startBlock = 83_000_000,
                currentBlock = 50_000_000
            ),
            blockchainType = BlockchainType.BinanceSmartChain,
        )

        assertEquals(AdapterState.Synced, adapter.balanceState)
    }

    @Test
    fun balanceState_nativeSyncedWhileForwardSyncing_returnsSynced() {
        val adapter = createAdapter(
            forwardSyncState = ForwardSyncState.Syncing(
                lastSyncedTip = 83_000_000,
                chainTipBlock = 83_000_500
            ),
            blockchainType = BlockchainType.BinanceSmartChain,
        )

        assertEquals(AdapterState.Synced, adapter.balanceState)
    }

    @Test
    fun balanceState_nativeSyncedWhileBothErc20SyncsActive_returnsSynced() {
        val adapter = createAdapter(
            historicalSyncState = HistoricalSyncState.Syncing(
                startBlock = 83_000_000,
                currentBlock = 40_000_000
            ),
            forwardSyncState = ForwardSyncState.Syncing(
                lastSyncedTip = 83_000_000,
                chainTipBlock = 83_001_000
            ),
            blockchainType = BlockchainType.BinanceSmartChain,
        )

        assertEquals(AdapterState.Synced, adapter.balanceState)
    }

    // --- Standard native sync state transitions ---

    @Test
    fun balanceState_notStarted_returnsConnecting() {
        val adapter = createAdapter(
            syncState = SyncState.NotSynced(SyncError.NotStarted()),
            txSyncState = SyncState.NotSynced(SyncError.NotStarted()),
        )

        assertEquals(AdapterState.Connecting, adapter.balanceState)
    }

    @Test
    fun balanceState_balanceNotSynced_returnsNotSynced() {
        val adapter = createAdapter(
            syncState = SyncState.NotSynced(Exception("RPC unavailable")),
        )

        assertTrue(adapter.balanceState is AdapterState.NotSynced)
    }

    @Test
    fun balanceState_balanceSyncing_returnsSyncing() {
        val adapter = createAdapter(syncState = SyncState.Syncing())

        assertTrue(adapter.balanceState is AdapterState.Syncing)
    }

    @Test
    fun balanceState_txSyncError_returnsNotSynced() {
        val adapter = createAdapter(
            txSyncState = SyncState.NotSynced(Exception("tx sync failed")),
        )

        assertTrue(adapter.balanceState is AdapterState.NotSynced)
    }

    @Test
    fun balanceState_txNotStarted_returnsSynced() {
        val adapter = createAdapter(
            txSyncState = SyncState.NotSynced(SyncError.NotStarted()),
        )

        assertEquals(AdapterState.Synced, adapter.balanceState)
    }
}
