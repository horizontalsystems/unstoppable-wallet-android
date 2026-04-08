package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmKitManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.core.onPollingStarted
import cash.p.terminal.core.onPollingStopped
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Flowable
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EvmTransactionsPollerTest {

    private lateinit var evmBlockchainManager: EvmBlockchainManager
    private lateinit var transactionAdapterManager: TransactionAdapterManager
    private lateinit var kitManager: EvmKitManager
    private lateinit var poller: EvmTransactionsPoller

    @Before
    fun setUp() {
        evmBlockchainManager = mockk(relaxed = true)
        transactionAdapterManager = mockk(relaxed = true)
        kitManager = mockk(relaxed = true)

        every { evmBlockchainManager.allBlockchains } returns listOf(
            mockk<Blockchain> { every { type } returns BlockchainType.Ethereum }
        )
        every { evmBlockchainManager.getEvmKitManager(BlockchainType.Ethereum) } returns kitManager

        poller = EvmTransactionsPoller(evmBlockchainManager, transactionAdapterManager)
    }

    @Test
    fun pollOnce_coordinatesManagerAndAdapter() = runTest {
        val wallet = mockk<Wallet>(relaxed = true) {
            every { token.blockchainType } returns BlockchainType.Ethereum
        }
        val transactionSource = wallet.transactionSource
        val record = mockk<TransactionRecord>()
        val adapter = mockk<ITransactionsAdapter> {
            every { transactionsState } returns AdapterState.Synced
            every { transactionsStateUpdatedFlowable } returns Flowable.never()
        }

        coEvery {
            adapter.getTransactions(null, null, 100, any(), null)
        } returns listOf(record)

        every {
            transactionAdapterManager.adaptersReadyFlow
        } returns MutableStateFlow(mapOf(transactionSource to adapter))

        val result = poller.pollOnce(listOf(wallet))

        assertEquals(1, result.size)
        assertEquals(record, result.first())
        coVerifyOrder {
            kitManager.startForPolling()
            adapter.getTransactions(null, null, 100, any(), null)
            kitManager.stopForPolling()
        }
    }

    @Test
    fun pollOnce_appInForeground_kitNotStopped() = runTest {
        val wallet = mockk<Wallet>(relaxed = true) {
            every { token.blockchainType } returns BlockchainType.Ethereum
        }
        val adapter = mockk<ITransactionsAdapter> {
            every { transactionsState } returns AdapterState.Synced
            every { transactionsStateUpdatedFlowable } returns Flowable.never()
        }
        coEvery { adapter.getTransactions(null, null, 100, any(), null) } returns emptyList()
        every { transactionAdapterManager.adaptersReadyFlow } returns
            MutableStateFlow(mapOf(wallet.transactionSource to adapter))

        val bgm = mockk<BackgroundManager> {
            every { stateFlow } returns MutableStateFlow(BackgroundManagerState.EnterForeground)
        }
        val sessionCount = AtomicInteger(0)
        var kitStopped = false
        coEvery { kitManager.startForPolling() } coAnswers { sessionCount.onPollingStarted {} }
        coEvery { kitManager.stopForPolling() } coAnswers {
            sessionCount.onPollingStopped(bgm) { kitStopped = true }
        }

        poller.pollOnce(listOf(wallet))

        assertFalse("Kit must not be stopped when app is in foreground", kitStopped)
    }

    @Test
    fun pollOnce_timeout_returnsEmpty() = runTest {
        val wallet = mockk<Wallet>(relaxed = true) {
            every { token.blockchainType } returns BlockchainType.Ethereum
        }

        coEvery { kitManager.startForPolling() } coAnswers {
            delay(60_000)
        }

        val result = poller.pollOnce(listOf(wallet))

        assertTrue(result.isEmpty())
    }
}
