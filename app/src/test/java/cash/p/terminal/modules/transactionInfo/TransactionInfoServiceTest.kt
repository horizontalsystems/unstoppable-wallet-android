package cash.p.terminal.modules.transactionInfo

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.AmlStatusManager
import cash.p.terminal.core.managers.PendingTransactionMatcher
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.core.usecase.UpdateSwapProviderTransactionsStatusUseCase
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.NftMetadataService
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.network.changenow.domain.entity.TransactionStatusEnum
import cash.p.terminal.network.swaprepository.SwapProvider
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import io.horizontalsystems.core.CurrencyManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.reactivex.subjects.PublishSubject
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionInfoServiceTest : KoinTest {

    private val dispatcher = StandardTestDispatcher()

    private val adapter = mockk<ITransactionsAdapter>(relaxed = true)
    private val marketKit = mockk<MarketKitWrapper>(relaxed = true)
    private val currencyManager = mockk<CurrencyManager>(relaxed = true)
    private val nftMetadataService = mockk<NftMetadataService>(relaxed = true)
    private val updateSwapProviderTransactionsStatusUseCase =
        mockk<UpdateSwapProviderTransactionsStatusUseCase>(relaxed = true)
    private val swapProviderTransactionsStorage =
        mockk<SwapProviderTransactionsStorage>(relaxed = true)
    private val transactionRecord = mockk<TransactionRecord>(relaxed = true)
    private val balanceHiddenManager = mockk<IBalanceHiddenManager>(relaxUnitFun = true)
    private val pendingTransactionMatcher = mockk<PendingTransactionMatcher>(relaxed = true)
    private val amlStatusManager = mockk<AmlStatusManager>(relaxed = true)

    private val lastBlockSubject = PublishSubject.create<Unit>()

    @get:Rule
    val koinRule = KoinTestRule.create {
        modules(
            module {
                single<IBalanceHiddenManager> { balanceHiddenManager }
                single { pendingTransactionMatcher }
                single { amlStatusManager }
            }
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        every { transactionRecord.uid } returns "tx-uid-1"
        every { transactionRecord.transactionHash } returns "0xabc"
        every { transactionRecord.timestamp } returns 1000L
        every { adapter.explorerTitle } returns "Explorer"
        every { adapter.getTransactionUrl(any()) } returns "https://explorer.com/tx/0xabc"
        every { adapter.lastBlockInfo } returns null
        every { adapter.lastBlockUpdatedFlowable } returns lastBlockSubject.toFlowable(
            io.reactivex.BackpressureStrategy.LATEST
        )
        every {
            adapter.getTransactionRecordsFlow(any(), any(), any())
        } returns MutableStateFlow(emptyList())
        every { nftMetadataService.assetsBriefMetadataFlow } returns MutableStateFlow(emptyMap())
        val hiddenFlow = MutableStateFlow(false)
        every { balanceHiddenManager.transactionInfoHiddenFlow(any()) } returns hiddenFlow
        every { balanceHiddenManager.transactionInfoHiddenFlow(any(), any()) } returns hiddenFlow
        every { balanceHiddenManager.transactionInfoHiddenFlowForWallet(any(), any()) } returns hiddenFlow
        every { balanceHiddenManager.isTransactionInfoHidden(any()) } returns false
        every { balanceHiddenManager.isTransactionInfoHidden(any(), any()) } returns false
        every { balanceHiddenManager.isTransactionInfoHiddenForWallet(any(), any()) } returns false
        every { balanceHiddenManager.balanceHidden } returns false
        every { amlStatusManager.statusUpdates } returns MutableSharedFlow()
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createFinishedSwapTransaction(transactionId: String = "cn-tx-1") =
        SwapProviderTransaction(
            date = 1000L,
            outgoingRecordUid = "tx-uid-1",
            transactionId = transactionId,
            status = TransactionStatusEnum.FINISHED.name.lowercase(),
            provider = SwapProvider.CHANGENOW,
            coinUidIn = "bitcoin",
            blockchainTypeIn = "bitcoin",
            amountIn = BigDecimal("0.1"),
            addressIn = "addr-in",
            coinUidOut = "ethereum",
            blockchainTypeOut = "ethereum",
            amountOut = BigDecimal("1.5"),
            addressOut = "addr-out"
        )

    private fun createService(
        userSwapTransactionId: String? = null
    ) = TransactionInfoService(
        initialTransactionRecord = transactionRecord,
        userSwapTransactionId = userSwapTransactionId,
        walletUid = "wallet-1",
        adapter = adapter,
        marketKit = marketKit,
        currencyManager = currencyManager,
        nftMetadataService = nftMetadataService,
        updateSwapProviderTransactionsStatusUseCase = updateSwapProviderTransactionsStatusUseCase,
        swapProviderTransactionsStorage = swapProviderTransactionsStorage,
        transactionStatusUrl = null
    )

    @Test
    fun start_nonSwapTransaction_externalStatusIsNull() = runTest(dispatcher) {
        val service = createService(userSwapTransactionId = null)
        backgroundScope.launch { service.start() }
        advanceUntilIdle()

        val item = service.transactionInfoItemFlow.first()
        assertEquals(null, item.externalStatus)
    }

    @Test
    fun start_completedSwap_firstEmissionHasCompletedStatus() = runTest(dispatcher) {
        val swap = createFinishedSwapTransaction()
        coEvery { swapProviderTransactionsStorage.getTransaction("cn-tx-1") } returns swap
        coEvery {
            updateSwapProviderTransactionsStatusUseCase.updateTransactionStatus("cn-tx-1")
        } returns TransactionStatusEnum.FINISHED

        val service = createService(userSwapTransactionId = "cn-tx-1")
        backgroundScope.launch { service.start() }
        advanceUntilIdle()

        val item = service.transactionInfoItemFlow.first()
        assertEquals(TransactionStatus.Completed, item.externalStatus)
    }

    @Test
    fun start_completedSwap_firstEmissionIncludesSwapAmounts() = runTest(dispatcher) {
        val swap = createFinishedSwapTransaction()
        coEvery { swapProviderTransactionsStorage.getTransaction("cn-tx-1") } returns swap
        coEvery {
            updateSwapProviderTransactionsStatusUseCase.updateTransactionStatus("cn-tx-1")
        } returns TransactionStatusEnum.FINISHED

        val service = createService(userSwapTransactionId = "cn-tx-1")
        backgroundScope.launch { service.start() }
        advanceUntilIdle()

        val item = service.transactionInfoItemFlow.first()
        assertEquals(BigDecimal("0.1"), item.swapAmountIn)
        assertEquals(BigDecimal("1.5"), item.swapAmountOut)
        assertEquals(SwapProvider.CHANGENOW, item.swapProvider)
        assertEquals(TransactionStatus.Completed, item.externalStatus)
    }

    @Test
    fun blockUpdate_completedSwap_doesNotRefetchStatus() = runTest(dispatcher) {
        val swap = createFinishedSwapTransaction()
        coEvery { swapProviderTransactionsStorage.getTransaction("cn-tx-1") } returns swap
        coEvery {
            updateSwapProviderTransactionsStatusUseCase.updateTransactionStatus("cn-tx-1")
        } returns TransactionStatusEnum.FINISHED

        val service = createService(userSwapTransactionId = "cn-tx-1")
        backgroundScope.launch { service.start() }
        advanceUntilIdle()

        // Wait for start() to complete and emit first item
        service.transactionInfoItemFlow.first()

        coVerify(exactly = 1) {
            updateSwapProviderTransactionsStatusUseCase.updateTransactionStatus("cn-tx-1")
        }

        // Simulate block updates
        lastBlockSubject.onNext(Unit)
        advanceUntilIdle()
        lastBlockSubject.onNext(Unit)
        advanceUntilIdle()

        // Should still be exactly 1 call — block updates did not trigger re-fetch
        coVerify(exactly = 1) {
            updateSwapProviderTransactionsStatusUseCase.updateTransactionStatus("cn-tx-1")
        }

        assertEquals(TransactionStatus.Completed, service.transactionInfoItem.externalStatus)
    }

    @Test
    fun blockUpdate_inProgressSwap_refetchesStatus() = runTest(dispatcher) {
        val swap = createFinishedSwapTransaction().copy(
            status = TransactionStatusEnum.EXCHANGING.name.lowercase()
        )
        coEvery { swapProviderTransactionsStorage.getTransaction("cn-tx-1") } returns swap
        coEvery {
            updateSwapProviderTransactionsStatusUseCase.updateTransactionStatus("cn-tx-1")
        } returns TransactionStatusEnum.EXCHANGING

        val service = createService(userSwapTransactionId = "cn-tx-1")
        backgroundScope.launch { service.start() }
        advanceUntilIdle()

        service.transactionInfoItemFlow.first()

        coVerify(exactly = 1) {
            updateSwapProviderTransactionsStatusUseCase.updateTransactionStatus("cn-tx-1")
        }

        lastBlockSubject.onNext(Unit)
        advanceUntilIdle()

        coVerify(exactly = 2) {
            updateSwapProviderTransactionsStatusUseCase.updateTransactionStatus("cn-tx-1")
        }
    }

    @Test
    fun blockUpdate_failedSwap_stillRefetchesStatus() = runTest(dispatcher) {
        val swap = createFinishedSwapTransaction().copy(
            status = TransactionStatusEnum.UNKNOWN.name.lowercase()
        )
        coEvery { swapProviderTransactionsStorage.getTransaction("cn-tx-1") } returns swap
        coEvery {
            updateSwapProviderTransactionsStatusUseCase.updateTransactionStatus("cn-tx-1")
        } returns TransactionStatusEnum.UNKNOWN

        val service = createService(userSwapTransactionId = "cn-tx-1")
        backgroundScope.launch { service.start() }
        advanceUntilIdle()

        service.transactionInfoItemFlow.first()

        coVerify(exactly = 1) {
            updateSwapProviderTransactionsStatusUseCase.updateTransactionStatus("cn-tx-1")
        }

        lastBlockSubject.onNext(Unit)
        advanceUntilIdle()

        coVerify(exactly = 2) {
            updateSwapProviderTransactionsStatusUseCase.updateTransactionStatus("cn-tx-1")
        }
    }
}
