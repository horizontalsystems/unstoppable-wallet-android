package cash.p.terminal.modules.transactions

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.managers.PendingTransactionRepository
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Verifies that TransactionRecordRepository discards stale data
 * from account A when switching to account B with identical filters.
 *
 * Models the actual race through the real code path:
 *   loadItems() -> TransactionAdapterWrapper.get() -> ITransactionsAdapter.getTransactions()
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionRecordRepositoryWalletSwitchTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun switchAccountWithSameFilters_slowGetTransactions_oldResultDiscarded() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        startKoin {
            modules(module {
                single { mockk<cash.p.terminal.core.managers.CoinManager>(relaxed = true) }
            })
        }

        val accountA = Account(
            id = "account-A", name = "A",
            type = mockk(relaxed = true), origin = AccountOrigin.Created, level = 0
        )
        val accountB = Account(
            id = "account-B", name = "B",
            type = mockk(relaxed = true), origin = AccountOrigin.Created, level = 0
        )
        val blockchainA = Blockchain(BlockchainType.Ethereum, "Ethereum", null)
        val blockchainB = Blockchain(BlockchainType.Bitcoin, "Bitcoin", null)
        val sourceA = TransactionSource(blockchainA, accountA, null)
        val sourceB = TransactionSource(blockchainB, accountB, null)
        val walletA = TransactionWallet(token = null, source = sourceA, badge = null)
        val walletB = TransactionWallet(token = null, source = sourceB, badge = null)

        val recordA = mockk<TransactionRecord>(relaxed = true) {
            every { uid } returns "record-A"
            every { source } returns sourceA
            every { timestamp } returns 1000L
            every { spam } returns false
            every { compareTo(any()) } returns 0
        }
        val recordB = mockk<TransactionRecord>(relaxed = true) {
            every { uid } returns "record-B"
            every { source } returns sourceB
            every { timestamp } returns 2000L
            every { spam } returns false
            every { compareTo(any()) } returns 0
        }

        val adapterAGate = CompletableDeferred<Unit>()

        val adapterA = mockk<ITransactionsAdapter>(relaxed = true) {
            coEvery { getTransactions(any(), any(), any(), any(), any()) } coAnswers {
                adapterAGate.await()
                listOf(recordA)
            }
            every { getTransactionRecordsFlow(any(), any(), any()) } returns emptyFlow()
            every { getTransactionUrl(any()) } returns ""
        }

        val adapterB = mockk<ITransactionsAdapter>(relaxed = true) {
            coEvery { getTransactions(any(), any(), any(), any(), any()) } returns listOf(recordB)
            every { getTransactionRecordsFlow(any(), any(), any()) } returns emptyFlow()
            every { getTransactionUrl(any()) } returns ""
        }

        val adapterManager = mockk<TransactionAdapterManager>(relaxed = true) {
            every { getAdapter(sourceA) } returns adapterA
            every { getAdapter(sourceB) } returns adapterB
        }

        val pendingRepository = mockk<PendingTransactionRepository>(relaxed = true) {
            every { getActivePendingFlow(any()) } returns emptyFlow()
            coEvery { getPendingForWallet(any()) } returns emptyList()
        }

        val repository = TransactionRecordRepository(
            adapterManager = adapterManager,
            swapProviderTransactionsStorage = mockk(relaxed = true),
            pendingRepository = pendingRepository,
            pendingConverter = mockk(relaxed = true),
            dispatcherProvider = TestDispatcherProvider(testDispatcher, this)
        )

        val emissions = mutableListOf<List<TransactionRecord>>()
        val collectorJob = launch(testDispatcher) {
            repository.itemsFlow.collect { records ->
                emissions.add(records)
            }
        }

        // Step 1: Set account A -> getTransactions() suspends on gate
        repository.set(
            transactionWallets = listOf(walletA),
            wallet = null,
            transactionType = FilterTransactionType.All,
            blockchain = null,
            contact = null
        )
        advanceUntilIdle()

        // Step 2: While A is still suspended, switch to B (same filters)
        repository.set(
            transactionWallets = listOf(walletB),
            wallet = null,
            transactionType = FilterTransactionType.All,
            blockchain = null,
            contact = null
        )
        advanceUntilIdle()

        // Step 3: Open the gate — adapter A's getTransactions() returns
        adapterAGate.complete(Unit)
        advanceUntilIdle()

        assertTrue(
            "record-B from new account should appear in emissions, " +
                "but got: ${emissions.map { it.map { r -> r.uid } }}",
            emissions.any { list -> list.any { it.uid == "record-B" } }
        )

        assertTrue(
            "record-A from old account should never appear in emissions, " +
                "but got: ${emissions.map { it.map { r -> r.uid } }}",
            emissions.none { list -> list.any { it.uid == "record-A" } }
        )

        collectorJob.cancel()
        repository.clear()
    }
}
