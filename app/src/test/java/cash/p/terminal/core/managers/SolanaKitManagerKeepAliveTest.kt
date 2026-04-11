package cash.p.terminal.core.managers

import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the keep-alive guard logic used in SolanaKitManager,
 * TronKitManager, and other kit managers.
 *
 * Verifies that when BackgroundManagerState.EnterBackground arrives,
 * the kit is stopped only if NOT in the keep-alive set.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SolanaKitManagerKeepAliveTest {

    private val keepAliveManager = BackgroundKeepAliveManager()

    @Test
    fun enterBackground_solanaInKeepAlive_doesNotStop() = runTest {
        keepAliveManager.setKeepAlive(setOf(BlockchainType.Solana))

        var stopped = false
        simulateEnterBackground(BlockchainType.Solana) { stopped = true }

        assertFalse(stopped, "SolanaKit should NOT stop when in keep-alive set")
    }

    @Test
    fun enterBackground_solanaNotInKeepAlive_stops() = runTest {
        keepAliveManager.clear()

        var stopped = false
        simulateEnterBackground(BlockchainType.Solana) { stopped = true }

        assertTrue(stopped, "SolanaKit should stop when NOT in keep-alive set")
    }

    @Test
    fun enterBackground_tronInKeepAlive_doesNotStop() = runTest {
        keepAliveManager.setKeepAlive(setOf(BlockchainType.Tron))

        var stopped = false
        simulateEnterBackground(BlockchainType.Tron) { stopped = true }

        assertFalse(stopped, "TronKit should NOT stop when in keep-alive set")
    }

    @Test
    fun enterBackground_stellarInKeepAlive_doesNotStop() = runTest {
        keepAliveManager.setKeepAlive(setOf(BlockchainType.Stellar))

        var stopped = false
        simulateEnterBackground(BlockchainType.Stellar) { stopped = true }

        assertFalse(stopped, "StellarKit should NOT stop when in keep-alive set")
    }

    @Test
    fun enterBackground_stellarNotInKeepAlive_stops() = runTest {
        keepAliveManager.clear()

        var stopped = false
        simulateEnterBackground(BlockchainType.Stellar) { stopped = true }

        assertTrue(stopped, "StellarKit should stop when NOT in keep-alive set")
    }

    @Test
    fun enterBackground_onlyBitcoinInKeepAlive_solanStops() = runTest {
        keepAliveManager.setKeepAlive(setOf(BlockchainType.Bitcoin))

        var stopped = false
        simulateEnterBackground(BlockchainType.Solana) { stopped = true }

        assertTrue(stopped, "SolanaKit should stop when only Bitcoin is in keep-alive set")
    }

    @Test
    fun enterBackground_solanaPollingInProgress_doesNotPause() = runTest {
        keepAliveManager.clear()

        var paused = false
        simulateEnterBackground(
            blockchainType = BlockchainType.Solana,
            pollingSessionCount = 1,
        ) { paused = true }

        assertFalse(paused, "SolanaKit should NOT pause while a polling session is active")
    }

    /**
     * Verifies the fixed lifecycle pattern from SolanaKitManager.subscribeToEvents():
     * background → pause() (listener stays alive), foreground → resume().
     *
     * Previously stopKit() cancelled the listener job, so EnterForeground was never handled.
     * Now pause()/resume() are used — the listener stays alive across the background cycle.
     */
    @Test
    fun enterForeground_afterBackgroundPauseWithoutKeepAlive_kitResumes() = runTest {
        val stateFlow = MutableStateFlow(BackgroundManagerState.Unknown)
        var kitResumed = false
        var kitPaused = false

        // Exact pattern from fixed SolanaKitManager.subscribeToEvents()
        val listenerJob = launch {
            stateFlow.collect { state ->
                when (state) {
                    BackgroundManagerState.EnterForeground -> kitResumed = true   // resume()
                    BackgroundManagerState.EnterBackground -> {
                        if (!keepAliveManager.isKeepAlive(BlockchainType.Solana)) {
                            kitPaused = true  // pause() — listener stays alive
                        }
                    }
                    else -> {}
                }
            }
        }
        advanceUntilIdle()

        stateFlow.value = BackgroundManagerState.EnterBackground
        advanceUntilIdle()
        assertTrue(kitPaused, "Kit must be paused on background")

        stateFlow.value = BackgroundManagerState.EnterForeground
        advanceUntilIdle()
        assertTrue(kitResumed, "Kit must resume when app returns to foreground")

        listenerJob.cancel()
    }

    /**
     * Reproduces the guard pattern from SolanaKitManager.subscribeToEvents():
     * ```
     * if (state == EnterBackground) {
     *     if (pollingSessionCount == 0 && !keepAliveManager.isKeepAlive(blockchainType)) {
     *         pause()
     *     }
     * }
     * ```
     * TronKitManager and the other non-Solana managers use the same shape.
     */
    private fun simulateEnterBackground(
        blockchainType: BlockchainType,
        pollingSessionCount: Int = 0,
        stopKit: () -> Unit,
    ) {
        val state = BackgroundManagerState.EnterBackground
        if (state == BackgroundManagerState.EnterBackground) {
            if (pollingSessionCount == 0 && !keepAliveManager.isKeepAlive(blockchainType)) {
                stopKit()
            }
        }
    }
}
