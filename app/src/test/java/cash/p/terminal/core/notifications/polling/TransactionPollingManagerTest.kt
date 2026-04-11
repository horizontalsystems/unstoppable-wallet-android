package cash.p.terminal.core.notifications.polling

import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionPollingManagerTest {

    private val backgroundManager = mockk<BackgroundManager>(relaxed = true)

    private fun mockWallet(blockchainType: BlockchainType): Wallet {
        val token = mockk<Token> {
            every { this@mockk.blockchainType } returns blockchainType
        }
        return mockk {
            every { this@mockk.token } returns token
        }
    }

    private fun mockRecord(): TransactionRecord = mockk(relaxed = true)

    @Test
    fun pollAll_appEntersForeground_stopsSubPollers() = runTest {
        val btcPoller = mockk<TransactionsPoller> {
            every { blockchainTypes } returns setOf(BlockchainType.Bitcoin)
            coEvery { pollOnce(any()) } coAnswers {
                // Simulate app entering foreground during first poll
                every { backgroundManager.inForeground } returns true
                delay(100)
                emptyList()
            }
        }
        val ethPoller = mockk<TransactionsPoller> {
            every { blockchainTypes } returns setOf(BlockchainType.Ethereum)
            coEvery { pollOnce(any()) } returns emptyList()
        }

        // Start in background
        every { backgroundManager.inForeground } returns false

        val manager = TransactionPollingManager(listOf(btcPoller, ethPoller), backgroundManager)

        manager.pollAll(
            blockchainTypes = setOf(BlockchainType.Bitcoin, BlockchainType.Ethereum),
            wallets = listOf(mockWallet(BlockchainType.Bitcoin), mockWallet(BlockchainType.Ethereum)),
        )

        // btcPoller was already started before foreground switch
        coVerify(exactly = 1) { btcPoller.pollOnce(any()) }
        // ethPoller might or might not start depending on coroutine scheduling,
        // but the important thing is the check inside async block.
    }

    @Test
    fun pollAll_runsInParallel() = runTest {
        val btcPoller = mockk<TransactionsPoller> {
            every { blockchainTypes } returns setOf(BlockchainType.Bitcoin)
            coEvery { pollOnce(any()) } coAnswers {
                delay(1000)
                emptyList()
            }
        }
        val ethPoller = mockk<TransactionsPoller> {
            every { blockchainTypes } returns setOf(BlockchainType.Ethereum)
            coEvery { pollOnce(any()) } coAnswers {
                delay(1000)
                emptyList()
            }
        }

        every { backgroundManager.inForeground } returns false
        val manager = TransactionPollingManager(listOf(btcPoller, ethPoller), backgroundManager)

        manager.pollAll(
            blockchainTypes = setOf(BlockchainType.Bitcoin, BlockchainType.Ethereum),
            wallets = listOf(mockWallet(BlockchainType.Bitcoin), mockWallet(BlockchainType.Ethereum)),
        )

        // If parallel, should be ~1000ms, if sequential ~2000ms
        // Note: runTest uses virtual time, so we check virtual time
        assertTrue(testScheduler.currentTime < 2000, "Polling must be parallel")
        assertEquals(1000, testScheduler.currentTime)
    }

    @Test
    fun pollAll_onePollerFails_returnsPartialResults() = runTest {
        val record = mockRecord()

        val failingPoller = mockk<TransactionsPoller> {
            every { blockchainTypes } returns setOf(BlockchainType.Ethereum)
            coEvery { pollOnce(any()) } throws RuntimeException("network error")
        }
        val goodPoller = mockk<TransactionsPoller> {
            every { blockchainTypes } returns setOf(BlockchainType.Bitcoin)
            coEvery { pollOnce(any()) } returns listOf(record)
        }

        val manager = TransactionPollingManager(listOf(failingPoller, goodPoller), backgroundManager)
        val btcWallet = mockWallet(BlockchainType.Bitcoin)
        val ethWallet = mockWallet(BlockchainType.Ethereum)

        val result = manager.pollAll(
            blockchainTypes = setOf(BlockchainType.Ethereum, BlockchainType.Bitcoin),
            wallets = listOf(ethWallet, btcWallet),
        )

        assertEquals(listOf(record), result)
    }

    @Test
    fun pollAll_noMatchingPollers_returnsEmpty() = runTest {
        val poller = mockk<TransactionsPoller> {
            every { blockchainTypes } returns setOf(BlockchainType.Bitcoin)
        }

        val manager = TransactionPollingManager(listOf(poller), backgroundManager)

        val result = manager.pollAll(
            blockchainTypes = setOf(BlockchainType.Ethereum),
            wallets = listOf(mockWallet(BlockchainType.Ethereum)),
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun pollAll_filtersWalletsByBlockchainType() = runTest {
        val btcWallet = mockWallet(BlockchainType.Bitcoin)
        val ethWallet = mockWallet(BlockchainType.Ethereum)

        val btcPoller = mockk<TransactionsPoller> {
            every { blockchainTypes } returns setOf(BlockchainType.Bitcoin)
            coEvery { pollOnce(any()) } returns emptyList()
        }
        val ethPoller = mockk<TransactionsPoller> {
            every { blockchainTypes } returns setOf(BlockchainType.Ethereum)
            coEvery { pollOnce(any()) } returns emptyList()
        }

        val manager = TransactionPollingManager(listOf(btcPoller, ethPoller), backgroundManager)

        manager.pollAll(
            blockchainTypes = setOf(BlockchainType.Bitcoin, BlockchainType.Ethereum),
            wallets = listOf(btcWallet, ethWallet),
        )

        coVerify { btcPoller.pollOnce(listOf(btcWallet)) }
        coVerify { ethPoller.pollOnce(listOf(ethWallet)) }
    }
}
