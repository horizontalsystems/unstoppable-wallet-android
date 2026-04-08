package cash.p.terminal.core.notifications.polling

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.SolanaKitManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SolanaPollingRecoveryTest {

    private val solanaKitManager = mockk<SolanaKitManager>(relaxed = true)
    private val transactionAdapterManager = mockk<TransactionAdapterManager>(relaxed = true)

    private val poller = SolanaTransactionsPoller(solanaKitManager, transactionAdapterManager)

    @Test
    fun pollOnce_wrapperAlive_readsDirectlyWithoutLifecycleCycling() = runTest {
        val record = mockk<TransactionRecord>()
        val wallet = mockk<Wallet>(relaxed = true) {
            every { token.blockchainType } returns BlockchainType.Solana
        }
        val adapter = mockk<ITransactionsAdapter> {
            every { transactionsState } returns AdapterState.Synced
            every { transactionsStateUpdatedFlowable } returns Flowable.never()
        }
        coEvery {
            adapter.getTransactions(null, null, 100, any(), null)
        } returns listOf(record)

        every {
            transactionAdapterManager.adaptersReadyFlow
        } returns MutableStateFlow(mapOf(wallet.transactionSource to adapter))
        every { solanaKitManager.solanaKitWrapper } returns mockk(relaxed = true)

        val result = poller.pollOnce(listOf(wallet))

        assertEquals(1, result.size)
        assertEquals(record, result.first())
    }

    @Test
    fun pollOnce_wrapperNull_returnsEmpty() = runTest {
        every { solanaKitManager.solanaKitWrapper } returns null

        val wallet = mockk<Wallet>(relaxed = true) {
            every { token.blockchainType } returns BlockchainType.Solana
        }

        val result = poller.pollOnce(listOf(wallet))

        assertEquals(0, result.size)
    }
}
