package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.MoneroKitManager
import cash.p.terminal.core.managers.MoneroKitWrapper
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.onPollingStartedSuspend
import cash.p.terminal.core.onPollingStoppedSuspend
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MoneroTransactionsPollerTest {

    private val kitManager = mockk<MoneroKitManager>(relaxed = true)
    private val transactionAdapterManager = mockk<TransactionAdapterManager>(relaxed = true)
    private val poller = MoneroTransactionsPoller(kitManager, transactionAdapterManager)

    private fun mockWallet() = mockk<Wallet>(relaxed = true) {
        every { token.blockchainType } returns BlockchainType.Monero
    }

    private fun mockSyncedWrapper(): MoneroKitWrapper {
        return mockk(relaxed = true) {
            every { syncState } returns MutableStateFlow(AdapterState.Synced)
        }
    }

    @Test
    fun pollOnce_nullWrapper_returnsEmpty() = runTest {
        every { kitManager.moneroKitWrapper } returns null

        val result = poller.pollOnce(listOf(mockWallet()))

        assertTrue(result.isEmpty())
    }

    @Test
    fun pollOnce_synced_returnsTransactions() = runTest {
        val wallet = mockWallet()
        val record = mockk<TransactionRecord>()
        val adapter = mockk<ITransactionsAdapter>(relaxed = true)
        coEvery { adapter.getTransactions(null, null, 100, any(), null) } returns listOf(record)
        every { transactionAdapterManager.adaptersReadyFlow } returns
            MutableStateFlow(mapOf(wallet.transactionSource to adapter))
        every { kitManager.moneroKitWrapper } returns mockSyncedWrapper()

        val result = poller.pollOnce(listOf(wallet))

        assertEquals(listOf(record), result)
        coVerifyOrder {
            kitManager.startForPolling()
            kitManager.stopForPolling()
        }
    }

    @Test
    fun pollOnce_timeout_returnsEmpty() = runTest {
        every { kitManager.moneroKitWrapper } returns mockk(relaxed = true) {
            every { syncState } returns MutableStateFlow(AdapterState.Syncing())
        }
        coEvery { kitManager.startForPolling() } coAnswers { delay(60_001) }

        val result = poller.pollOnce(listOf(mockWallet()))

        assertTrue(result.isEmpty())
    }

    @Test
    fun pollOnce_appInForeground_kitNotStopped() = runTest {
        val wallet = mockWallet()
        val adapter = mockk<ITransactionsAdapter>(relaxed = true)
        coEvery { adapter.getTransactions(null, null, 100, any(), null) } returns emptyList()
        every { transactionAdapterManager.adaptersReadyFlow } returns
            MutableStateFlow(mapOf(wallet.transactionSource to adapter))
        every { kitManager.moneroKitWrapper } returns mockSyncedWrapper()

        val bgm = mockk<BackgroundManager> {
            every { stateFlow } returns MutableStateFlow(BackgroundManagerState.EnterForeground)
        }
        val sessionCount = AtomicInteger(0)
        var kitStopped = false
        coEvery { kitManager.startForPolling() } coAnswers { sessionCount.onPollingStartedSuspend {} }
        coEvery { kitManager.stopForPolling() } coAnswers {
            sessionCount.onPollingStoppedSuspend(bgm) { kitStopped = true }
        }

        poller.pollOnce(listOf(wallet))

        assertFalse(kitStopped, "Kit must not be stopped when app is in foreground")
    }
}
