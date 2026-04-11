package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.TonKitManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.onPollingStarted
import cash.p.terminal.core.onPollingStopped
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
import io.reactivex.Flowable
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
class TonTransactionsPollerTest {

    private val kitManager = mockk<TonKitManager>(relaxed = true)
    private val transactionAdapterManager = mockk<TransactionAdapterManager>(relaxed = true)
    private val poller = TonTransactionsPoller(kitManager, transactionAdapterManager)

    private fun mockWallet() = mockk<Wallet>(relaxed = true) {
        every { token.blockchainType } returns BlockchainType.Ton
    }

    private fun mockAdapter() = mockk<ITransactionsAdapter>(relaxed = true) {
        every { transactionsState } returns AdapterState.Synced
        every { transactionsStateUpdatedFlowable } returns Flowable.never()
    }

    @Test
    fun pollOnce_synced_returnsTransactions() = runTest {
        val wallet = mockWallet()
        val record = mockk<TransactionRecord>()
        val adapter = mockAdapter()
        coEvery { adapter.getTransactions(null, null, 100, any(), null) } returns listOf(record)
        every { transactionAdapterManager.adaptersReadyFlow } returns
            MutableStateFlow(mapOf(wallet.transactionSource to adapter))

        val result = poller.pollOnce(listOf(wallet))

        assertEquals(listOf(record), result)
        coVerifyOrder {
            kitManager.startForPolling()
            kitManager.stopForPolling()
        }
    }

    @Test
    fun pollOnce_timeout_returnsEmpty() = runTest {
        coEvery { kitManager.startForPolling() } coAnswers { delay(60_001) }

        val result = poller.pollOnce(listOf(mockWallet()))

        assertTrue(result.isEmpty())
    }

    @Test
    fun pollOnce_appInForeground_kitNotStopped() = runTest {
        val wallet = mockWallet()
        val adapter = mockAdapter()
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

        assertFalse(kitStopped, "Kit must not be stopped when app is in foreground")
    }
}
