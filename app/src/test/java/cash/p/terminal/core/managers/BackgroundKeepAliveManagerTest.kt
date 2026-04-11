package cash.p.terminal.core.managers

import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundKeepAliveManagerTest {

    private lateinit var manager: BackgroundKeepAliveManager

    @Before
    fun setUp() {
        manager = BackgroundKeepAliveManager()
    }

    @Test
    fun isKeepAlive_emptySet_returnsFalse() {
        assertFalse(manager.isKeepAlive(BlockchainType.Bitcoin))
    }

    @Test
    fun isKeepAlive_afterSetKeepAlive_returnsTrue() {
        manager.setKeepAlive(setOf(BlockchainType.Bitcoin))

        assertTrue(manager.isKeepAlive(BlockchainType.Bitcoin))
    }

    @Test
    fun isKeepAlive_differentType_returnsFalse() {
        manager.setKeepAlive(setOf(BlockchainType.Bitcoin))

        assertFalse(manager.isKeepAlive(BlockchainType.Ethereum))
    }

    @Test
    fun clear_afterSetKeepAlive_returnsEmptySet() {
        manager.setKeepAlive(setOf(BlockchainType.Bitcoin, BlockchainType.Ethereum))

        manager.clear()

        assertFalse(manager.isKeepAlive(BlockchainType.Bitcoin))
        assertFalse(manager.isKeepAlive(BlockchainType.Ethereum))
    }

    @Test
    fun keepAliveBlockchains_afterSetKeepAlive_emitsCorrectSet() = runTest {
        val expected = setOf(BlockchainType.Bitcoin, BlockchainType.Ethereum)

        manager.setKeepAlive(expected)

        assertEquals(expected, manager.keepAliveBlockchains.first())
    }
}
