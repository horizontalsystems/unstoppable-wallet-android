package cash.p.terminal.core.managers

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.storage.SpamAddressStorage
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SpamManagerTest {

    private val adaptersReadyFlow = MutableStateFlow<Map<TransactionSource, ITransactionsAdapter>>(emptyMap())
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var spamManager: SpamManager

    @Before
    fun setUp() {
        val localStorage = mockk<ILocalStorage>(relaxed = true) {
            every { hideSuspiciousTransactions } returns false
        }
        val transactionAdapterManager = mockk<TransactionAdapterManager>(relaxed = true) {
            every { adaptersReadyFlow } returns this@SpamManagerTest.adaptersReadyFlow
        }

        spamManager = SpamManager(
            localStorage = localStorage,
            spamAddressStorage = mockk<SpamAddressStorage>(relaxed = true),
            transactionAdapterManager = transactionAdapterManager,
            dispatcherProvider = TestDispatcherProvider(testDispatcher, testScope)
        )
    }

    @After
    fun tearDown() {
        spamManager.close()
    }

    @Test
    fun subscribeToAdapters_existingAdapterOnAdditionalSourceEmission_doesNotDuplicateSubscription() = testScope.runTest {
        val firstSource = transactionSource(BlockchainType.Ethereum)
        val secondSource = transactionSource(BlockchainType.Solana)
        val firstSubscriptionCount = AtomicInteger()
        val secondSubscriptionCount = AtomicInteger()
        val firstAdapter = transactionsAdapter(subscriptionCount = firstSubscriptionCount)
        val secondAdapter = transactionsAdapter(subscriptionCount = secondSubscriptionCount)

        emitAdapters(mapOf(firstSource to firstAdapter))
        assertEquals(1, firstSubscriptionCount.get())

        emitAdapters(mapOf(firstSource to firstAdapter, secondSource to secondAdapter))

        assertEquals(1, firstSubscriptionCount.get())
        assertEquals(1, secondSubscriptionCount.get())
    }

    @Test
    fun subscribeToAdapters_removedAdapter_cancelsSubscription() = testScope.runTest {
        val source = transactionSource(BlockchainType.Ethereum)
        val cancellationCount = AtomicInteger()
        val adapter = transactionsAdapter(cancellationCount = cancellationCount)

        emitAdapters(mapOf(source to adapter))
        assertEquals(1, adapter.subscriptionCount.get())

        emitAdapters(emptyMap())

        assertEquals(1, cancellationCount.get())
    }

    private fun emitAdapters(adapters: Map<TransactionSource, ITransactionsAdapter>) {
        adaptersReadyFlow.value = adapters
        testScope.advanceUntilIdle()
    }

    private fun transactionSource(blockchainType: BlockchainType): TransactionSource =
        TransactionSource(
            blockchain = mockk<Blockchain> {
                every { type } returns blockchainType
            },
            account = mockk(relaxed = true),
            meta = null
        )

    private fun transactionsAdapter(
        subscriptionCount: AtomicInteger = AtomicInteger(),
        cancellationCount: AtomicInteger = AtomicInteger()
    ): SubscriptionCountingAdapter {
        val flowable = Flowable.never<Unit>()
            .doOnSubscribe { subscriptionCount.incrementAndGet() }
            .doOnCancel { cancellationCount.incrementAndGet() }

        val adapter = mockk<ITransactionsAdapter>(relaxed = true) {
            every { transactionsStateUpdatedFlowable } returns flowable
        }

        return SubscriptionCountingAdapter(adapter, subscriptionCount)
    }

    private data class SubscriptionCountingAdapter(
        val adapter: ITransactionsAdapter,
        val subscriptionCount: AtomicInteger
    ) : ITransactionsAdapter by adapter
}
