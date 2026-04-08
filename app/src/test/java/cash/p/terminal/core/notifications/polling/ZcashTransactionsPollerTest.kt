package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.adapters.zcash.ZcashAdapter
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ZcashTransactionsPollerTest {

    private lateinit var transactionAdapterManager: TransactionAdapterManager
    private lateinit var zcashAdapter: ZcashAdapter
    private lateinit var poller: ZcashTransactionsPoller

    @Before
    fun setUp() {
        transactionAdapterManager = mockk(relaxed = true)
        zcashAdapter = mockk(relaxed = true) {
            every { transactionsState } returns AdapterState.Synced
            every { transactionsStateUpdatedFlowable } returns Flowable.never()
        }
        poller = ZcashTransactionsPoller(transactionAdapterManager)
    }

    private fun mockWallet() = mockk<Wallet>(relaxed = true) {
        every { token.blockchainType } returns BlockchainType.Zcash
    }

    @Test
    fun pollOnce_synced_returnsTransactionsAndCallsStartStop() = runTest {
        val wallet = mockWallet()
        val record = mockk<TransactionRecord>()
        coEvery { zcashAdapter.getTransactions(null, null, 100, any(), null) } returns listOf(record)
        every { transactionAdapterManager.adaptersReadyFlow } returns
            MutableStateFlow(mapOf(wallet.transactionSource to zcashAdapter))

        val result = poller.pollOnce(listOf(wallet))

        assertEquals(listOf(record), result)
        verifyOrder {
            zcashAdapter.startForPolling()
            zcashAdapter.stopForPolling()
        }
    }

    @Test
    fun pollOnce_adapterNotZcashAdapter_skipsWallet() = runTest {
        val wallet = mockWallet()
        val nonZcashAdapter = mockk<ITransactionsAdapter>(relaxed = true)
        every { transactionAdapterManager.adaptersReadyFlow } returns
            MutableStateFlow(mapOf(wallet.transactionSource to nonZcashAdapter))

        val result = poller.pollOnce(listOf(wallet))

        assertTrue(result.isEmpty())
    }

    @Test
    fun pollOnce_timeout_returnsEmpty() = runTest {
        val wallet = mockWallet()
        every { zcashAdapter.transactionsState } returns AdapterState.Syncing()
        every { transactionAdapterManager.adaptersReadyFlow } returns
            MutableStateFlow(mapOf(wallet.transactionSource to zcashAdapter))

        val result = poller.pollOnce(listOf(wallet))

        assertTrue(result.isEmpty())
    }

    @Test
    fun pollOnce_appInForeground_kitNotStopped() = runTest {
        val wallet = mockWallet()
        coEvery { zcashAdapter.getTransactions(null, null, 100, any(), null) } returns emptyList()
        every { transactionAdapterManager.adaptersReadyFlow } returns
            MutableStateFlow(mapOf(wallet.transactionSource to zcashAdapter))

        val bgm = mockk<BackgroundManager> {
            every { stateFlow } returns MutableStateFlow(BackgroundManagerState.EnterForeground)
        }
        val sessionCount = AtomicInteger(0)
        var kitStopped = false
        every { zcashAdapter.startForPolling() } answers { sessionCount.onPollingStarted {} }
        every { zcashAdapter.stopForPolling() } answers {
            sessionCount.onPollingStopped(bgm) { kitStopped = true }
        }

        poller.pollOnce(listOf(wallet))

        assertFalse(kitStopped, "Kit must not be stopped when app is in foreground")
    }
}
