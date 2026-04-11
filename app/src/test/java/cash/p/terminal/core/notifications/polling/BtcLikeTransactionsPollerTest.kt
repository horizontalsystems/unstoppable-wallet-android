package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.adapters.BitcoinBaseAdapter
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.onPollingStarted
import cash.p.terminal.core.onPollingStopped
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.entities.Blockchain
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
class BtcLikeTransactionsPollerTest {

    private lateinit var btcBlockchainManager: BtcBlockchainManager
    private lateinit var transactionAdapterManager: TransactionAdapterManager
    private lateinit var btcAdapter: BitcoinBaseAdapter
    private lateinit var poller: BtcLikeTransactionsPoller

    @Before
    fun setUp() {
        btcBlockchainManager = mockk(relaxed = true) {
            every { allBlockchains } returns listOf(
                mockk<Blockchain> { every { type } returns BlockchainType.Bitcoin }
            )
        }
        transactionAdapterManager = mockk(relaxed = true)
        btcAdapter = mockk(relaxed = true) {
            every { transactionsState } returns AdapterState.Synced
            every { transactionsStateUpdatedFlowable } returns Flowable.never()
        }
        poller = BtcLikeTransactionsPoller(btcBlockchainManager, transactionAdapterManager)
    }

    private fun mockWallet() = mockk<Wallet>(relaxed = true) {
        every { token.blockchainType } returns BlockchainType.Bitcoin
    }

    @Test
    fun pollOnce_synced_returnsTransactionsAndCallsStartStop() = runTest {
        val wallet = mockWallet()
        val record = mockk<TransactionRecord>()
        coEvery { btcAdapter.getTransactions(null, null, 100, any(), null) } returns listOf(record)
        every { transactionAdapterManager.adaptersReadyFlow } returns
            MutableStateFlow(mapOf(wallet.transactionSource to btcAdapter))

        val result = poller.pollOnce(listOf(wallet))

        assertEquals(listOf(record), result)
        verifyOrder {
            btcAdapter.startForPolling()
            btcAdapter.stopForPolling()
        }
    }

    @Test
    fun pollOnce_adapterNotBitcoinBaseAdapter_skipsWallet() = runTest {
        val wallet = mockWallet()
        val nonBtcAdapter = mockk<ITransactionsAdapter>(relaxed = true)
        every { transactionAdapterManager.adaptersReadyFlow } returns
            MutableStateFlow(mapOf(wallet.transactionSource to nonBtcAdapter))

        val result = poller.pollOnce(listOf(wallet))

        assertTrue(result.isEmpty())
    }

    @Test
    fun pollOnce_timeout_returnsEmpty() = runTest {
        val wallet = mockWallet()
        every { btcAdapter.transactionsState } returns AdapterState.Syncing()
        every { transactionAdapterManager.adaptersReadyFlow } returns
            MutableStateFlow(mapOf(wallet.transactionSource to btcAdapter))

        val result = poller.pollOnce(listOf(wallet))

        assertTrue(result.isEmpty())
    }

    @Test
    fun pollOnce_appInForeground_kitNotStopped() = runTest {
        val wallet = mockWallet()
        coEvery { btcAdapter.getTransactions(null, null, 100, any(), null) } returns emptyList()
        every { transactionAdapterManager.adaptersReadyFlow } returns
            MutableStateFlow(mapOf(wallet.transactionSource to btcAdapter))

        val bgm = mockk<BackgroundManager> {
            every { stateFlow } returns MutableStateFlow(BackgroundManagerState.EnterForeground)
        }
        val sessionCount = AtomicInteger(0)
        var kitStopped = false
        every { btcAdapter.startForPolling() } answers { sessionCount.onPollingStarted {} }
        every { btcAdapter.stopForPolling() } answers {
            sessionCount.onPollingStopped(bgm) { kitStopped = true }
        }

        poller.pollOnce(listOf(wallet))

        assertFalse(kitStopped, "Kit must not be stopped when app is in foreground")
    }
}
