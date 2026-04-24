package cash.p.terminal.core.managers

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.factories.AdapterFactory
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionAdapterManagerTest {

    private interface WalletTransactionsAdapter : IAdapter, ITransactionsAdapter

    private val adaptersReadyProcessor = BehaviorProcessor.create<Map<Wallet, IAdapter>>()
    private val initializationInProgressFlow = MutableStateFlow(false)
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var adapterFactory: AdapterFactory
    private lateinit var transactionAdapterManager: TransactionAdapterManager

    @Before
    fun setUp() {
        adapterFactory = mockk(relaxed = true)

        val adapterManager = mockk<IAdapterManager>(relaxed = true) {
            every { adaptersReadyObservable } returns adaptersReadyProcessor
            every { initializationInProgressFlow } returns this@TransactionAdapterManagerTest.initializationInProgressFlow
        }

        transactionAdapterManager = TransactionAdapterManager(
            adapterManager = adapterManager,
            adapterFactory = adapterFactory,
            dispatcherProvider = TestDispatcherProvider(testDispatcher, testScope)
        )
    }

    @After
    fun tearDown() {
        transactionAdapterManager.close()
    }

    @Test
    fun initAdapters_sameSourceNewWalletAdapter_replacesTransactionsAdapter() = testScope.runTest {
        val source = transactionSource()
        val wallet = wallet(source)
        val firstAdapter = walletTransactionsAdapter()
        val secondAdapter = walletTransactionsAdapter()

        emitAdapters(mapOf(wallet to firstAdapter))
        assertSame(firstAdapter, transactionAdapterManager.getAdapter(source))

        emitAdapters(mapOf(wallet to secondAdapter))

        assertSame(secondAdapter, transactionAdapterManager.getAdapter(source))
    }

    @Test
    fun initAdapters_sameSourceSameWalletAdapter_reusesTransactionsAdapter() = testScope.runTest {
        val source = transactionSource()
        val wallet = wallet(source)
        val adapter = walletTransactionsAdapter()

        emitAdapters(mapOf(wallet to adapter))
        emitAdapters(mapOf(wallet to adapter))

        assertSame(adapter, transactionAdapterManager.getAdapter(source))
    }

    @Test
    fun initAdapters_sameSourceSameWalletAdaptersDifferentOrder_reusesTransactionsAdapter() = testScope.runTest {
        val source = transactionSource(BlockchainType.Ethereum)
        val firstWallet = wallet(source)
        val secondWallet = wallet(source)
        val firstAdapter = walletAdapter()
        val secondAdapter = walletAdapter()
        val transactionsAdapter = transactionsAdapter()

        coEvery {
            adapterFactory.evmTransactionsAdapter(source, BlockchainType.Ethereum)
        } returns transactionsAdapter

        emitAdapters(linkedMapOf(firstWallet to firstAdapter, secondWallet to secondAdapter))
        assertSame(transactionsAdapter, transactionAdapterManager.getAdapter(source))

        emitAdapters(linkedMapOf(secondWallet to secondAdapter, firstWallet to firstAdapter))

        assertSame(transactionsAdapter, transactionAdapterManager.getAdapter(source))
        coVerify(exactly = 1) {
            adapterFactory.evmTransactionsAdapter(source, BlockchainType.Ethereum)
        }
    }

    @Test
    fun initAdapters_sameSourceWalletSetChanged_unlinksReplacedTransactionSource() = testScope.runTest {
        val source = transactionSource(BlockchainType.Ethereum)
        val firstWallet = wallet(source)
        val secondWallet = wallet(source)
        val firstAdapter = walletAdapter()
        val secondAdapter = walletAdapter()
        val firstTxAdapter = transactionsAdapter()
        val secondTxAdapter = transactionsAdapter()

        coEvery {
            adapterFactory.evmTransactionsAdapter(source, BlockchainType.Ethereum)
        } returnsMany listOf(firstTxAdapter, secondTxAdapter)

        emitAdapters(mapOf(firstWallet to firstAdapter))
        emitAdapters(mapOf(firstWallet to firstAdapter, secondWallet to secondAdapter))

        coVerify(exactly = 1) {
            adapterFactory.unlinkAdapter(source)
        }
    }

    @Test
    fun initAdapters_sameSourceSameWalletSet_doesNotUnlinkTransactionSource() = testScope.runTest {
        val source = transactionSource(BlockchainType.Ethereum)
        val wallet = wallet(source)
        val adapter = walletAdapter()
        val txAdapter = transactionsAdapter()

        coEvery {
            adapterFactory.evmTransactionsAdapter(source, BlockchainType.Ethereum)
        } returns txAdapter

        emitAdapters(mapOf(wallet to adapter))
        emitAdapters(mapOf(wallet to adapter))

        coVerify(exactly = 0) {
            adapterFactory.unlinkAdapter(source)
        }
    }

    @Test
    fun initAdapters_sameSourceNewEqualWalletAdapter_replacesTransactionsAdapter() = testScope.runTest {
        val source = transactionSource(BlockchainType.Ethereum)
        val wallet = wallet(source)
        val firstAdapter = EqualWalletAdapter()
        val secondAdapter = EqualWalletAdapter()
        val firstTransactionsAdapter = transactionsAdapter()
        val secondTransactionsAdapter = transactionsAdapter()

        coEvery {
            adapterFactory.evmTransactionsAdapter(source, BlockchainType.Ethereum)
        } returnsMany listOf(firstTransactionsAdapter, secondTransactionsAdapter)

        emitAdapters(mapOf(wallet to firstAdapter))
        assertSame(firstTransactionsAdapter, transactionAdapterManager.getAdapter(source))

        emitAdapters(mapOf(wallet to secondAdapter))

        assertSame(secondTransactionsAdapter, transactionAdapterManager.getAdapter(source))
    }

    private fun emitAdapters(adapters: Map<Wallet, IAdapter>) {
        adaptersReadyProcessor.onNext(adapters)
        testScope.advanceUntilIdle()
    }

    private fun wallet(source: TransactionSource): Wallet =
        mockk(relaxed = true) {
            every { transactionSource } returns source
        }

    private fun transactionSource(blockchainType: BlockchainType = BlockchainType.Bitcoin): TransactionSource =
        TransactionSource(
            blockchain = mockk<Blockchain> {
                every { type } returns blockchainType
            },
            account = mockk(relaxed = true),
            meta = null
        )

    private fun walletAdapter(): IAdapter =
        mockk(relaxed = true)

    private fun transactionsAdapter(): ITransactionsAdapter =
        mockk(relaxed = true) {
            every { transactionsStateUpdatedFlowable } returns Flowable.never()
            every { lastBlockUpdatedFlowable } returns Flowable.never()
        }

    private fun walletTransactionsAdapter(): WalletTransactionsAdapter =
        mockk(relaxed = true) {
            every { transactionsStateUpdatedFlowable } returns Flowable.never()
            every { lastBlockUpdatedFlowable } returns Flowable.never()
        }

    private class EqualWalletAdapter : IAdapter {
        override val debugInfo: String = ""
        override val statusInfo: Map<String, Any> = emptyMap()

        override fun start() = Unit

        override fun stop() = Unit

        override suspend fun refresh() = Unit

        override fun equals(other: Any?): Boolean =
            other is EqualWalletAdapter

        override fun hashCode(): Int = 1
    }
}
