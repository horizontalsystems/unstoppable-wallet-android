package cash.p.terminal.modules.transactions

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.managers.PendingTransactionMatcher
import cash.p.terminal.core.managers.PendingTransactionRepository
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.contacts.model.ContactAddress
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
            pendingTransactionMatcher = PendingTransactionMatcher(),
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

    @Test
    fun switchContactInSwapFilter_extraSwapAdaptersUseNewContact() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        startKoin {
            modules(module {
                single { mockk<cash.p.terminal.core.managers.CoinManager>(relaxed = true) }
            })
        }

        val token = createToken()
        val source = createSource(accountId = "account-1", blockchain = token.blockchain)
        val wallet = TransactionWallet(token = token, source = source, badge = null)
        val contactA = createContact(uid = "contact-A", blockchain = token.blockchain, address = "bc1-contact-a")
        val contactB = createContact(uid = "contact-B", blockchain = token.blockchain, address = "bc1-contact-b")
        val recordA = createBitcoinOutgoingRecord(
            token = token,
            source = source,
            uid = "record-A",
            timestamp = 1_715_000_000,
            amount = BigDecimal("-0.00000563"),
            toAddress = contactA.addresses.first().address,
        )
        val recordB = createBitcoinOutgoingRecord(
            token = token,
            source = source,
            uid = "record-B",
            timestamp = 1_715_000_010,
            amount = BigDecimal("-0.00000703"),
            toAddress = contactB.addresses.first().address,
        )

        val adapter = mockk<ITransactionsAdapter>(relaxed = true) {
            coEvery {
                getTransactions(any(), token, any(), FilterTransactionType.Swap, any())
            } returns emptyList()
            coEvery {
                getTransactions(any(), token, any(), FilterTransactionType.Outgoing, contactA.addresses.first().address)
            } returns listOf(recordA)
            coEvery {
                getTransactions(any(), token, any(), FilterTransactionType.Outgoing, contactB.addresses.first().address)
            } returns listOf(recordB)
            every { getTransactionRecordsFlow(any(), any(), any()) } returns emptyFlow()
            every { getTransactionUrl(any()) } returns ""
        }

        val adapterManager = mockk<TransactionAdapterManager>(relaxed = true) {
            every { getAdapter(source) } returns adapter
        }

        val pendingRepository = mockk<PendingTransactionRepository>(relaxed = true) {
            every { getActivePendingFlow(any()) } returns emptyFlow()
            coEvery { getPendingForWallet(any()) } returns emptyList()
        }

        val swapProviderTransactionsStorage = mockk<SwapProviderTransactionsStorage>(relaxed = true) {
            every { getByOutgoingRecordUid("record-A") } returns mockk(relaxed = true)
            every { getByOutgoingRecordUid("record-B") } returns mockk(relaxed = true)
        }

        val repository = TransactionRecordRepository(
            adapterManager = adapterManager,
            swapProviderTransactionsStorage = swapProviderTransactionsStorage,
            pendingRepository = pendingRepository,
            pendingConverter = mockk(relaxed = true),
            pendingTransactionMatcher = PendingTransactionMatcher(),
            dispatcherProvider = TestDispatcherProvider(testDispatcher, this)
        )

        val emissions = mutableListOf<List<String>>()
        val collectorJob = launch(testDispatcher) {
            repository.itemsFlow.collect { records ->
                emissions.add(records.map { it.uid })
            }
        }

        repository.set(
            transactionWallets = listOf(wallet),
            wallet = null,
            transactionType = FilterTransactionType.Swap,
            blockchain = null,
            contact = contactA
        )
        advanceUntilIdle()

        repository.set(
            transactionWallets = listOf(wallet),
            wallet = null,
            transactionType = FilterTransactionType.Swap,
            blockchain = null,
            contact = contactB
        )
        advanceUntilIdle()

        assertTrue(
            "record-A should appear for the first contact, but got: $emissions",
            emissions.any { it == listOf("record-A") }
        )
        assertEquals(listOf("record-B"), emissions.last())

        collectorJob.cancel()
        repository.clear()
    }

    private fun createToken(): Token {
        val coin = Coin(uid = "bitcoin", name = "Bitcoin", code = "BTC")
        val blockchain = Blockchain(BlockchainType.Bitcoin, "Bitcoin", null)
        return Token(
            coin = coin,
            blockchain = blockchain,
            type = TokenType.Derived(TokenType.Derivation.Bip86),
            decimals = 8
        )
    }

    private fun createSource(accountId: String, blockchain: Blockchain): TransactionSource {
        val account = Account(
            id = accountId,
            name = accountId,
            type = mockk(relaxed = true),
            origin = AccountOrigin.Created,
            level = 0
        )
        return TransactionSource(blockchain = blockchain, account = account, meta = null)
    }

    private fun createContact(uid: String, blockchain: Blockchain, address: String) = Contact(
        uid = uid,
        name = uid,
        addresses = listOf(ContactAddress(blockchain = blockchain, address = address))
    )

    private fun createBitcoinOutgoingRecord(
        token: Token,
        source: TransactionSource,
        uid: String,
        timestamp: Long,
        amount: BigDecimal,
        toAddress: String,
    ) = BitcoinTransactionRecord(
        token = token,
        amount = amount,
        to = listOf(toAddress),
        from = null,
        changeAddresses = emptyList(),
        uid = uid,
        transactionHash = uid,
        transactionIndex = 0,
        blockHeight = null,
        confirmationsThreshold = 1,
        timestamp = timestamp,
        failed = false,
        memo = null,
        source = source,
        sentToSelf = false,
        transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING,
        fee = null,
        lockInfo = null,
        conflictingHash = null,
        showRawTransaction = true,
        replaceable = false,
    )
}
