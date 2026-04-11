package cash.p.terminal.core.usecase

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.storage.PendingMultiSwapStorage
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.PendingMultiSwap
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.ton.TonTransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.network.swaprepository.SwapProvider
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SyncPendingMultiSwapUseCaseTest {

    private val pendingMultiSwapStorage = mockk<PendingMultiSwapStorage>(relaxed = true)
    private val swapProviderTransactionsStorage =
        mockk<SwapProviderTransactionsStorage>(relaxed = true)
    private val walletManager = mockk<IWalletManager>(relaxed = true)
    private val transactionAdapterManager = mockk<TransactionAdapterManager>(relaxed = true)
    private val adapterManager = mockk<IAdapterManager>(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()

    private val testScope = CoroutineScope(dispatcher)

    private val updateSwapProviderTransactionsStatusUseCase =
        mockk<UpdateSwapProviderTransactionsStatusUseCase>(relaxed = true)
    private val accountManager = mockk<IAccountManager>(relaxed = true) {
        every { activeAccount } returns Account(
            id = "test-account",
            name = "Test",
            type = AccountType.EvmAddress("0x1"),
            origin = AccountOrigin.Created,
            level = 0,
        )
    }

    private val useCase = SyncPendingMultiSwapUseCase(
        pendingMultiSwapStorage,
        swapProviderTransactionsStorage,
        TestDispatcherProvider(dispatcher, testScope),
        walletManager,
        transactionAdapterManager,
        adapterManager,
        updateSwapProviderTransactionsStatusUseCase,
        accountManager,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun swap(
        id: String = "swap-1",
        leg1Status: String = PendingMultiSwap.STATUS_EXECUTING,
        leg1IsOffChain: Boolean = true,
        leg1TransactionId: String? = "0xabc",
        leg1AmountOut: BigDecimal? = null,
        leg2Status: String = PendingMultiSwap.STATUS_PENDING,
        leg2IsOffChain: Boolean? = false,
        leg2TransactionId: String? = null,
        leg2AmountOut: BigDecimal? = null,
        createdAt: Long = 1000L,
        leg2StartedAt: Long? = null,
        leg1ProviderTransactionId: String? = null,
        leg2ProviderTransactionId: String? = null,
    ) = PendingMultiSwap(
        id = id,
        accountId = "test-account",
        createdAt = createdAt,
        coinUidIn = "binancecoin",
        blockchainTypeIn = "binance-smart-chain",
        amountIn = BigDecimal("1.0"),
        coinUidIntermediate = "the-open-network",
        blockchainTypeIntermediate = "the-open-network",
        coinUidOut = "pirate",
        blockchainTypeOut = "the-open-network",
        leg1ProviderId = "changenow",
        leg1IsOffChain = leg1IsOffChain,
        leg1TransactionId = leg1TransactionId,
        leg1AmountOut = leg1AmountOut,
        leg1Status = leg1Status,
        leg2ProviderId = "stonfi",
        leg2IsOffChain = leg2IsOffChain,
        leg2TransactionId = leg2TransactionId,
        leg2AmountOut = leg2AmountOut,
        leg2Status = leg2Status,
        expectedAmountOut = BigDecimal("100"),
        leg2StartedAt = leg2StartedAt,
        leg1ProviderTransactionId = leg1ProviderTransactionId,
        leg2ProviderTransactionId = leg2ProviderTransactionId,
    )

    private fun swapProviderTx(
        status: String = "finished",
        outgoingRecordUid: String = "0xabc",
        amountOutReal: BigDecimal? = BigDecimal("1.5"),
    ) = SwapProviderTransaction(
        date = 1000L,
        outgoingRecordUid = outgoingRecordUid,
        transactionId = "cn-id-123",
        status = status,
        provider = SwapProvider.CHANGENOW,
        coinUidIn = "binancecoin",
        blockchainTypeIn = "binance-smart-chain",
        amountIn = BigDecimal("1.0"),
        addressIn = "0x123",
        coinUidOut = "the-open-network",
        blockchainTypeOut = "the-open-network",
        amountOut = BigDecimal("1.5"),
        addressOut = "UQ123",
        amountOutReal = amountOutReal,
    )

    @Test
    fun leg1OffChainFinished_updatesLegToCompleted() = runTest(dispatcher) {
        val swap = swap()
        val providerTx = swapProviderTx()

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("0xabc") } returns providerTx

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("1.5"),
                transactionId = "0xabc",
            )
        }
    }

    @Test
    fun leg1OffChainNotFinished_doesNotUpdate() = runTest(dispatcher) {
        val swap = swap()
        val providerTx = swapProviderTx(status = "exchanging")

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("0xabc") } returns providerTx

        useCase()

        coVerify(exactly = 0) {
            pendingMultiSwapStorage.updateLeg1(any(), any(), any(), any())
        }
    }

    @Test
    fun leg1OnChain_noWallet_doesNotUpdate() = runTest(dispatcher) {
        val swap = swap(leg1IsOffChain = false)

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        every { walletManager.activeWallets } returns listOf()

        useCase()

        coVerify(exactly = 0) {
            pendingMultiSwapStorage.updateLeg1(any(), any(), any(), any())
        }
    }

    @Test
    fun leg1OnChain_outgoingFailed_usesInputWallet_marksFailed() = runTest(dispatcher) {
        val swap = swap(leg1IsOffChain = false, leg1TransactionId = "0xfailed-send")

        val inputSource = mockk<TransactionSource>()
        val inputWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "binancecoin"
            every { token.blockchainType } returns BlockchainType.fromUid("binance-smart-chain")
            every { transactionSource } returns inputSource
        }
        val outputWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "the-open-network"
            every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
        }
        val txAdapter = mockk<ITransactionsAdapter>(relaxed = true)
        val failedRecord = mockk<TransactionRecord>(relaxed = true) {
            every { transactionHash } returns "0xfailed-send"
            every { failed } returns true
        }

        every { walletManager.activeWallets } returns listOf(inputWallet, outputWallet)
        // Only the INPUT source should be queried for outgoing failure
        every { transactionAdapterManager.getAdapter(inputSource) } returns txAdapter
        coEvery {
            txAdapter.getTransactions(
                any(),
                any(),
                any(),
                eq(FilterTransactionType.Outgoing),
                any()
            )
        } returns listOf(failedRecord)
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg1Status = PendingMultiSwap.STATUS_FAILED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_FAILED,
                amountOut = null,
                transactionId = "0xfailed-send",
            )
        }
        // Verify it used the INPUT source, not output
        coVerify { transactionAdapterManager.getAdapter(inputSource) }
    }

    @Test
    fun leg1OnChain_incomingMatch_marksCompleted() = runTest(dispatcher) {
        val swap = swap(
            leg1IsOffChain = false,
            leg1TransactionId = null,
            leg1AmountOut = BigDecimal("5.0")
        )

        val outputSource = mockk<TransactionSource>()
        val outputWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "the-open-network"
            every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
            every { transactionSource } returns outputSource
        }
        val txAdapter = mockk<ITransactionsAdapter>(relaxed = true)
        val mainValue = mockk<TransactionValue>(relaxed = true) {
            every { decimalValue } returns BigDecimal("4.8")
        }
        val incomingRecord = mockk<TransactionRecord>(relaxed = true) {
            every { transactionHash } returns "ton-incoming-123"
            every { timestamp } returns (swap.createdAt / 1000) + 60
            every { this@mockk.mainValue } returns mainValue
        }

        every { walletManager.activeWallets } returns listOf(outputWallet)
        every { transactionAdapterManager.getAdapter(outputSource) } returns txAdapter
        coEvery {
            txAdapter.getTransactions(any(), any(), any(), eq(FilterTransactionType.All), any())
        } returns listOf(incomingRecord)
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg1Status = PendingMultiSwap.STATUS_COMPLETED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("4.8"),
                transactionId = null,
            )
        }
    }

    @Test
    fun leg1AlreadyCompleted_doesNotUpdateStatus() = runTest(dispatcher) {
        val swap = swap(leg1Status = PendingMultiSwap.STATUS_COMPLETED)

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)

        useCase()

        coVerify(exactly = 0) {
            pendingMultiSwapStorage.updateLeg1(any(), any(), any(), any())
        }
    }

    @Test
    fun leg1NoTransactionId_doesNotSync() = runTest(dispatcher) {
        val swap = swap(leg1TransactionId = null)

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)

        useCase()

        coVerify(exactly = 0) {
            pendingMultiSwapStorage.updateLeg1(any(), any(), any(), any())
        }
    }

    @Test
    fun leg1NoSwapProviderTransaction_doesNotSync() = runTest(dispatcher) {
        val swap = swap()

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("0xabc") } returns null

        useCase()

        coVerify(exactly = 0) {
            pendingMultiSwapStorage.updateLeg1(any(), any(), any(), any())
        }
    }

    @Test
    fun leg1Finished_amountOutRealNull_fallsBackToLegAmountOut() = runTest(dispatcher) {
        val swap = swap(leg1AmountOut = BigDecimal("2.0"))
        val providerTx = swapProviderTx(amountOutReal = null)

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("0xabc") } returns providerTx

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("2.0"),
                transactionId = "0xabc",
            )
        }
    }

    @Test
    fun leg2OffChainFinished_updatesLegToCompleted() = runTest(dispatcher) {
        val swap = swap(
            leg1Status = PendingMultiSwap.STATUS_COMPLETED,
            leg2Status = PendingMultiSwap.STATUS_EXECUTING,
            leg2IsOffChain = true,
            leg2TransactionId = "0xdef",
        )
        val providerTx =
            swapProviderTx(outgoingRecordUid = "0xdef", amountOutReal = BigDecimal("99"))

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("0xdef") } returns providerTx

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg2(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("99"),
                transactionId = "0xdef",
            )
        }
    }

    @Test
    fun leg2OnChain_noWallet_doesNotUpdate() = runTest(dispatcher) {
        val swap = swap(
            leg1Status = PendingMultiSwap.STATUS_COMPLETED,
            leg2Status = PendingMultiSwap.STATUS_EXECUTING,
            leg2IsOffChain = false,
            leg2TransactionId = "0xdef",
        )

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        every { walletManager.activeWallets } returns listOf()

        useCase()

        coVerify(exactly = 0) {
            pendingMultiSwapStorage.updateLeg2(any(), any(), any(), any())
        }
    }

    @Test
    fun leg2Pending_doesNotSync() = runTest(dispatcher) {
        val swap = swap(
            leg1Status = PendingMultiSwap.STATUS_COMPLETED,
            leg2Status = PendingMultiSwap.STATUS_PENDING,
            leg2IsOffChain = true,
        )

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)

        useCase()

        coVerify(exactly = 0) {
            pendingMultiSwapStorage.updateLeg2(any(), any(), any(), any())
        }
    }

    @Test
    fun multipleSwaps_syncsEachIndependently() = runTest(dispatcher) {
        val swap1 = swap(id = "swap-1", leg1TransactionId = "0xaaa")
        val swap2 = swap(
            id = "swap-2",
            leg1Status = PendingMultiSwap.STATUS_COMPLETED,
            leg2Status = PendingMultiSwap.STATUS_EXECUTING,
            leg2IsOffChain = true,
            leg2TransactionId = "0xbbb",
        )

        val providerTx1 = swapProviderTx(outgoingRecordUid = "0xaaa")
        val providerTx2 =
            swapProviderTx(outgoingRecordUid = "0xbbb", amountOutReal = BigDecimal("50"))

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap1, swap2)
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("0xaaa") } returns providerTx1
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("0xbbb") } returns providerTx2

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("1.5"),
                transactionId = "0xaaa",
            )
        }
        coVerify {
            pendingMultiSwapStorage.updateLeg2(
                id = "swap-2",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("50"),
                transactionId = "0xbbb",
            )
        }
    }

    @Test
    fun noActiveAccount_skipsSync() = runTest(dispatcher) {
        every { accountManager.activeAccount } returns null

        useCase()

        coVerify(exactly = 0) {
            pendingMultiSwapStorage.getAllOnceByAccountId(any())
            pendingMultiSwapStorage.updateLeg1(any(), any(), any(), any())
            pendingMultiSwapStorage.updateLeg2(any(), any(), any(), any())
            pendingMultiSwapStorage.delete(any())
        }
    }

    @Test
    fun noSwaps_doesNothing() = runTest(dispatcher) {
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns emptyList()

        useCase()

        coVerify(exactly = 0) {
            pendingMultiSwapStorage.updateLeg1(any(), any(), any(), any())
            pendingMultiSwapStorage.updateLeg2(any(), any(), any(), any())
            pendingMultiSwapStorage.delete(any())
        }
    }

    @Test
    fun bothLegsCompleted_deletesImmediately() = runTest(dispatcher) {
        val swap = swap(
            leg1Status = PendingMultiSwap.STATUS_COMPLETED,
            leg2Status = PendingMultiSwap.STATUS_COMPLETED,
        )

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)

        useCase()

        coVerify {
            pendingMultiSwapStorage.delete("swap-1")
        }
    }

    @Test
    fun leg1Failed_deletesImmediately() = runTest(dispatcher) {
        val swap = swap(
            leg1Status = PendingMultiSwap.STATUS_FAILED,
        )

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)

        useCase()

        coVerify {
            pendingMultiSwapStorage.delete("swap-1")
        }
    }

    @Test
    fun leg2StillExecuting_doesNotDelete() = runTest(dispatcher) {
        val swap = swap(
            leg1Status = PendingMultiSwap.STATUS_COMPLETED,
            leg2Status = PendingMultiSwap.STATUS_EXECUTING,
            leg2IsOffChain = false,
        )

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)

        useCase()

        coVerify(exactly = 0) {
            pendingMultiSwapStorage.delete(any())
        }
    }

    @Test
    fun leg2OnChain_outgoingFailed_usesIntermediateWallet_marksFailed() = runTest(dispatcher) {
        val swap = swap(
            leg1Status = PendingMultiSwap.STATUS_COMPLETED,
            leg2Status = PendingMultiSwap.STATUS_EXECUTING,
            leg2IsOffChain = false,
            leg2TransactionId = "ton-failed-swap",
        )

        val intermediateSource = mockk<TransactionSource>()
        val intermediateWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "the-open-network"
            every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
            every { transactionSource } returns intermediateSource
        }
        val txAdapter = mockk<ITransactionsAdapter>(relaxed = true)
        val failedRecord = mockk<TransactionRecord>(relaxed = true) {
            every { transactionHash } returns "ton-failed-swap"
            every { failed } returns true
        }

        every { walletManager.activeWallets } returns listOf(intermediateWallet)
        every { transactionAdapterManager.getAdapter(intermediateSource) } returns txAdapter
        coEvery {
            txAdapter.getTransactions(
                any(),
                any(),
                any(),
                eq(FilterTransactionType.Outgoing),
                any()
            )
        } returns listOf(failedRecord)
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg2Status = PendingMultiSwap.STATUS_FAILED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg2(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_FAILED,
                amountOut = null,
                transactionId = "ton-failed-swap",
            )
        }
        coVerify { transactionAdapterManager.getAdapter(intermediateSource) }
    }

    @Test
    fun leg1OnChain_outgoingFailedOnSecondPage_marksFailed() = runTest(dispatcher) {
        val swap = swap(leg1IsOffChain = false, leg1TransactionId = "0xfailed-deep")

        val inputSource = mockk<TransactionSource>()
        val inputWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "binancecoin"
            every { token.blockchainType } returns BlockchainType.fromUid("binance-smart-chain")
            every { transactionSource } returns inputSource
        }
        val txAdapter = mockk<ITransactionsAdapter>(relaxed = true)

        val page1Records = (1..50).map { i ->
            mockk<TransactionRecord>(relaxed = true) {
                every { transactionHash } returns "other-tx-$i"
                every { failed } returns false
            }
        }
        val failedRecord = mockk<TransactionRecord>(relaxed = true) {
            every { transactionHash } returns "0xfailed-deep"
            every { failed } returns true
        }

        every { walletManager.activeWallets } returns listOf(inputWallet)
        every { transactionAdapterManager.getAdapter(inputSource) } returns txAdapter
        coEvery {
            txAdapter.getTransactions(
                isNull(),
                any(),
                any(),
                eq(FilterTransactionType.Outgoing),
                any()
            )
        } returns page1Records
        coEvery {
            txAdapter.getTransactions(
                isNull(inverse = true),
                any(),
                any(),
                eq(FilterTransactionType.Outgoing),
                any()
            )
        } returns listOf(failedRecord)
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg1Status = PendingMultiSwap.STATUS_FAILED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_FAILED,
                amountOut = null,
                transactionId = "0xfailed-deep",
            )
        }
    }

    @Test
    fun leg2OnChain_outgoingFailedOnSecondPage_marksFailed() = runTest(dispatcher) {
        val swap = swap(
            leg1Status = PendingMultiSwap.STATUS_COMPLETED,
            leg2Status = PendingMultiSwap.STATUS_EXECUTING,
            leg2IsOffChain = false,
            leg2TransactionId = "ton-failed-deep",
        )

        val intermediateSource = mockk<TransactionSource>()
        val intermediateWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "the-open-network"
            every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
            every { transactionSource } returns intermediateSource
        }
        val txAdapter = mockk<ITransactionsAdapter>(relaxed = true)

        val page1Records = (1..50).map { i ->
            mockk<TransactionRecord>(relaxed = true) {
                every { transactionHash } returns "other-tx-$i"
                every { failed } returns false
            }
        }
        val failedRecord = mockk<TransactionRecord>(relaxed = true) {
            every { transactionHash } returns "ton-failed-deep"
            every { failed } returns true
        }

        every { walletManager.activeWallets } returns listOf(intermediateWallet)
        every { transactionAdapterManager.getAdapter(intermediateSource) } returns txAdapter
        coEvery {
            txAdapter.getTransactions(
                isNull(),
                any(),
                any(),
                eq(FilterTransactionType.Outgoing),
                any()
            )
        } returns page1Records
        coEvery {
            txAdapter.getTransactions(
                isNull(inverse = true),
                any(),
                any(),
                eq(FilterTransactionType.Outgoing),
                any()
            )
        } returns listOf(failedRecord)
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg2Status = PendingMultiSwap.STATUS_FAILED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg2(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_FAILED,
                amountOut = null,
                transactionId = "ton-failed-deep",
            )
        }
    }

    @Test
    fun leg2OnChain_incomingMatchOnSecondPage_marksCompleted() = runTest(dispatcher) {
        val swap = swap(
            leg1Status = PendingMultiSwap.STATUS_COMPLETED,
            leg2Status = PendingMultiSwap.STATUS_EXECUTING,
            leg2IsOffChain = false,
            leg2TransactionId = null,
            leg2AmountOut = BigDecimal("100"),
        )

        val outputSource = mockk<TransactionSource>()
        val outputWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "pirate"
            every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
            every { transactionSource } returns outputSource
        }
        val txAdapter = mockk<ITransactionsAdapter>(relaxed = true)

        // First page: 50 records all within time window but no amount match (size == 50 so pagination continues)
        val oldRecords = (1..50).map { i ->
            mockk<TransactionRecord>(relaxed = true) {
                every { timestamp } returns (swap.createdAt / 1000) + 60 + i.toLong()
                every { mainValue } returns null
            }
        }
        // Second page: contains the match
        val mainValue = mockk<TransactionValue>(relaxed = true) {
            every { decimalValue } returns BigDecimal("98")
        }
        val matchRecord = mockk<TransactionRecord>(relaxed = true) {
            every { transactionHash } returns "pirate-incoming"
            every { timestamp } returns (swap.createdAt / 1000) + 120
            every { this@mockk.mainValue } returns mainValue
        }

        coEvery {
            txAdapter.getTransactions(any(), any(), any(), eq(FilterTransactionType.All), any())
        } returnsMany listOf(oldRecords, listOf(matchRecord))

        every { walletManager.activeWallets } returns listOf(outputWallet)
        every { transactionAdapterManager.getAdapter(outputSource) } returns txAdapter
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg2Status = PendingMultiSwap.STATUS_COMPLETED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg2(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("98"),
                transactionId = null,
            )
        }
    }

    @Test
    fun leg2OnChain_expectedAmountMissing_singleIncomingInTightWindow_marksCompleted() =
        runTest(dispatcher) {
            val startTime = 1_000_000_000L
            val swap = swap(
                leg1Status = PendingMultiSwap.STATUS_COMPLETED,
                leg2Status = PendingMultiSwap.STATUS_EXECUTING,
                leg2IsOffChain = false,
                leg2TransactionId = null,
                leg2AmountOut = null,
                leg2StartedAt = startTime,
            )

            val outputSource = mockk<TransactionSource>()
            val outputWallet = mockk<Wallet>(relaxed = true) {
                every { coin.uid } returns "pirate"
                every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
                every { transactionSource } returns outputSource
            }
            val txAdapter = mockk<ITransactionsAdapter>(relaxed = true)
            val mainValue = mockk<TransactionValue>(relaxed = true) {
                every { decimalValue } returns BigDecimal("42")
            }
            val singleRecord = mockk<TransactionRecord>(relaxed = true) {
                every { timestamp } returns (startTime / 1000) + 120 // 2 min after start, within 10min window
                every { this@mockk.mainValue } returns mainValue
            }

            every { walletManager.activeWallets } returns listOf(outputWallet)
            every { transactionAdapterManager.getAdapter(outputSource) } returns txAdapter
            coEvery {
                txAdapter.getTransactions(any(), any(), any(), eq(FilterTransactionType.All), any())
            } returns listOf(singleRecord)
            coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
            coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg2Status = PendingMultiSwap.STATUS_COMPLETED)

            useCase()

            coVerify {
                pendingMultiSwapStorage.updateLeg2(
                    id = "swap-1",
                    status = PendingMultiSwap.STATUS_COMPLETED,
                    amountOut = BigDecimal("42"),
                    transactionId = null,
                )
            }
        }

    @Test
    fun leg2OnChain_expectedAmountMissing_multipleIncomingInTightWindow_doesNotComplete() =
        runTest(dispatcher) {
            val startTime = 1_000_000_000L
            val swap = swap(
                leg1Status = PendingMultiSwap.STATUS_COMPLETED,
                leg2Status = PendingMultiSwap.STATUS_EXECUTING,
                leg2IsOffChain = false,
                leg2TransactionId = null,
                leg2AmountOut = null,
                leg2StartedAt = startTime,
            )

            val outputSource = mockk<TransactionSource>()
            val outputWallet = mockk<Wallet>(relaxed = true) {
                every { coin.uid } returns "pirate"
                every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
                every { transactionSource } returns outputSource
            }
            val txAdapter = mockk<ITransactionsAdapter>(relaxed = true)
            val record1 = mockk<TransactionRecord>(relaxed = true) {
                every { timestamp } returns (startTime / 1000) + 60
                every { mainValue } returns mockk(relaxed = true) {
                    every { decimalValue } returns BigDecimal(
                        "10"
                    )
                }
            }
            val record2 = mockk<TransactionRecord>(relaxed = true) {
                every { timestamp } returns (startTime / 1000) + 120
                every { mainValue } returns mockk(relaxed = true) {
                    every { decimalValue } returns BigDecimal(
                        "20"
                    )
                }
            }

            every { walletManager.activeWallets } returns listOf(outputWallet)
            every { transactionAdapterManager.getAdapter(outputSource) } returns txAdapter
            coEvery {
                txAdapter.getTransactions(any(), any(), any(), eq(FilterTransactionType.All), any())
            } returns listOf(record1, record2)
            coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)

            useCase()

            coVerify(exactly = 0) {
                pendingMultiSwapStorage.updateLeg2(any(), any(), any(), any())
            }
        }

    @Test
    fun leg1OffChain_legacyFallback_usesGetReceiveAddressForWallet() = runTest(dispatcher) {
        val swap = swap(
            leg1TransactionId = null,
            leg1ProviderTransactionId = null,
            leg1AmountOut = BigDecimal("1.5"),
        )

        val intermediateWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "the-open-network"
            every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
        }
        val providerTx = swapProviderTx()

        every { walletManager.activeWallets } returns listOf(intermediateWallet)
        // getReceiveAddressForWallet returns address even when no adapter is available
        coEvery { adapterManager.getReceiveAddressForWallet(intermediateWallet) } returns "UQ-fallback-addr"
        every {
            swapProviderTransactionsStorage.getByProviderAndTokenOut(
                provider = SwapProvider.CHANGENOW,
                coinUidOut = "the-open-network",
                blockchainTypeOut = "the-open-network",
                addressOut = "UQ-fallback-addr",
                expectedAmount = BigDecimal("1.5"),
                legStartTime = any(),
            )
        } returns providerTx

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg1Status = PendingMultiSwap.STATUS_COMPLETED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("1.5"),
                transactionId = null,
            )
        }
    }

    @Test
    fun leg1OffChain_providerTxId_findsAndCompletes() = runTest(dispatcher) {
        val swap = swap(leg1ProviderTransactionId = "cn-123", leg1TransactionId = null)
        val providerTx = swapProviderTx()

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        coEvery { swapProviderTransactionsStorage.getTransaction("cn-123") } returns providerTx
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg1Status = PendingMultiSwap.STATUS_COMPLETED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("1.5"),
                transactionId = null,
            )
        }
    }

    @Test
    fun leg1OffChain_failed_setsStatusFailed() = runTest(dispatcher) {
        val swap = swap()
        val providerTx = swapProviderTx(status = "failed")

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("0xabc") } returns providerTx
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg1Status = PendingMultiSwap.STATUS_FAILED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_FAILED,
                amountOut = any(),
                transactionId = "0xabc",
            )
        }
    }

    @Test
    fun leg1OffChain_refunded_setsStatusFailed() = runTest(dispatcher) {
        val swap = swap()
        val providerTx = swapProviderTx(status = "refunded")

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("0xabc") } returns providerTx
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg1Status = PendingMultiSwap.STATUS_FAILED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_FAILED,
                amountOut = any(),
                transactionId = "0xabc",
            )
        }
    }

    // --- update-then-delete in same invoke cycle ---

    @Test
    fun leg2OffChainFinished_updatesAndDeletesInSameCycle() = runTest(dispatcher) {
        val swap = swap(
            leg1Status = PendingMultiSwap.STATUS_COMPLETED,
            leg2Status = PendingMultiSwap.STATUS_EXECUTING,
            leg2IsOffChain = true,
            leg2TransactionId = "0xdef",
        )
        val completedSwap = swap.copy(leg2Status = PendingMultiSwap.STATUS_COMPLETED)
        val providerTx =
            swapProviderTx(outgoingRecordUid = "0xdef", amountOutReal = BigDecimal("99"))

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returnsMany listOf(
            listOf(swap),
            listOf(completedSwap)
        )
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("0xdef") } returns providerTx

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg2(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("99"),
                transactionId = "0xdef",
            )
        }
        coVerify {
            pendingMultiSwapStorage.delete("swap-1")
        }
    }

    @Test
    fun leg1OffChainFailed_deletesInSameCycle() = runTest(dispatcher) {
        val swap = swap(leg1TransactionId = "0xabc")
        val failedSwap = swap.copy(leg1Status = PendingMultiSwap.STATUS_FAILED)
        val providerTx = swapProviderTx(status = "failed")

        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returnsMany listOf(
            listOf(swap),
            listOf(failedSwap)
        )
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("0xabc") } returns providerTx

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_FAILED,
                amountOut = any(),
                transactionId = "0xabc",
            )
        }
        coVerify {
            pendingMultiSwapStorage.delete("swap-1")
        }
    }

    // --- backfill leg1InfoRecordUid after completion ---

    @Test
    fun syncLeg1_completedWithoutInfoRecordUid_offChainBackfillsNavigationUid() =
        runTest(dispatcher) {
            val swap = swap(
                leg1Status = PendingMultiSwap.STATUS_COMPLETED,
                leg1IsOffChain = true,
                leg1TransactionId = "0xabc",
                leg1ProviderTransactionId = "cn-123",
            )
            val providerTx = swapProviderTx(
                outgoingRecordUid = "0xabc",
                amountOutReal = BigDecimal("1.5"),
            ).copy(incomingRecordUid = "incoming-uid-456")

            coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
            coEvery { swapProviderTransactionsStorage.getTransaction("cn-123") } returns providerTx

            useCase()

            coVerify {
                pendingMultiSwapStorage.setLeg1InfoRecordUid("swap-1", "incoming-uid-456")
            }
        }

    @Test
    fun syncLeg1_completedWithoutInfoRecordUid_offChainNoIncomingUid_fallsBackToTransactionId() =
        runTest(dispatcher) {
            val swap = swap(
                leg1Status = PendingMultiSwap.STATUS_COMPLETED,
                leg1IsOffChain = true,
                leg1TransactionId = "0xabc",
                leg1ProviderTransactionId = "cn-123",
            )
            val providerTx = swapProviderTx(
                outgoingRecordUid = "0xabc",
                amountOutReal = BigDecimal("1.5"),
            ) // incomingRecordUid is null by default

            coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
            coEvery { swapProviderTransactionsStorage.getTransaction("cn-123") } returns providerTx

            useCase()

            coVerify {
                pendingMultiSwapStorage.setLeg1InfoRecordUid("swap-1", "0xabc")
            }
        }

    @Test
    fun syncLeg1_completedWithoutInfoRecordUid_onChainBackfillsNavigationUid() =
        runTest(dispatcher) {
            val swap = swap(
                leg1Status = PendingMultiSwap.STATUS_COMPLETED,
                leg1IsOffChain = false,
                leg1TransactionId = null,
                leg1AmountOut = BigDecimal("5.0"),
            )

            val outputSource = mockk<TransactionSource>()
            val outputWallet = mockk<Wallet>(relaxed = true) {
                every { coin.uid } returns "the-open-network"
                every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
                every { transactionSource } returns outputSource
            }
            val txAdapter = mockk<ITransactionsAdapter>(relaxed = true)
            val mainValue = mockk<TransactionValue>(relaxed = true) {
                every { decimalValue } returns BigDecimal("4.8")
            }
            val incomingRecord = mockk<TransactionRecord>(relaxed = true) {
                every { uid } returns "ton-record-uid-789"
                every { timestamp } returns (swap.createdAt / 1000) + 60
                every { this@mockk.mainValue } returns mainValue
            }

            every { walletManager.activeWallets } returns listOf(outputWallet)
            every { transactionAdapterManager.getAdapter(outputSource) } returns txAdapter
            coEvery {
                txAdapter.getTransactions(any(), any(), any(), eq(FilterTransactionType.All), any())
            } returns listOf(incomingRecord)
            coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)

            useCase()

            coVerify {
                pendingMultiSwapStorage.setLeg1InfoRecordUid("swap-1", "ton-record-uid-789")
            }
        }

    @Test
    fun leg1OnChain_txTimestampSlightlyBeforeCreatedAt_stillMatches() = runTest(dispatcher) {
        val createdAt = 1774071095000L // millis
        val swap = swap(
            leg1IsOffChain = false,
            leg1TransactionId = null,
            leg1AmountOut = BigDecimal("3.93"),
            createdAt = createdAt,
        )

        val outputSource = mockk<TransactionSource>()
        val outputWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "the-open-network"
            every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
            every { transactionSource } returns outputSource
        }
        val txAdapter = mockk<ITransactionsAdapter>(relaxed = true)
        val mainValue = mockk<TransactionValue>(relaxed = true) {
            every { decimalValue } returns BigDecimal("3.93")
        }
        // Transaction timestamp 2 seconds BEFORE createdAt
        val record = mockk<TransactionRecord>(relaxed = true) {
            every { uid } returns "swap-tx-uid"
            every { timestamp } returns (createdAt / 1000) - 2
            every { this@mockk.mainValue } returns mainValue
        }

        every { walletManager.activeWallets } returns listOf(outputWallet)
        every { transactionAdapterManager.getAdapter(outputSource) } returns txAdapter
        coEvery {
            txAdapter.getTransactions(any(), any(), any(), eq(FilterTransactionType.All), any())
        } returns listOf(record)
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg1Status = PendingMultiSwap.STATUS_COMPLETED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("3.93"),
                transactionId = null,
            )
        }
    }

    // --- transaction status filtering ---

    @Test
    fun leg1OnChain_pendingTransaction_doesNotComplete() = runTest(dispatcher) {
        val swap = swap(
            leg1IsOffChain = false,
            leg1TransactionId = null,
            leg1AmountOut = BigDecimal("5.0")
        )

        val outputSource = mockk<TransactionSource>()
        val outputWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "the-open-network"
            every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
            every { transactionSource } returns outputSource
        }
        val txAdapter = mockk<ITransactionsAdapter>(relaxed = true) {
            every { lastBlockInfo } returns LastBlockInfo(100)
        }
        val mainValue = mockk<TransactionValue>(relaxed = true) {
            every { decimalValue } returns BigDecimal("4.8")
        }
        val pendingRecord = mockk<TransactionRecord>(relaxed = true) {
            every { uid } returns "pending-tx"
            every { timestamp } returns (swap.createdAt / 1000) + 10
            every { this@mockk.mainValue } returns mainValue
            every { failed } returns false
            every { status(any()) } returns TransactionStatus.Pending
        }

        every { walletManager.activeWallets } returns listOf(outputWallet)
        every { transactionAdapterManager.getAdapter(outputSource) } returns txAdapter
        coEvery {
            txAdapter.getTransactions(any(), any(), any(), eq(FilterTransactionType.All), any())
        } returns listOf(pendingRecord)
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)

        useCase()

        coVerify(exactly = 0) {
            pendingMultiSwapStorage.updateLeg1(any(), any(), any(), any())
        }
    }

    @Test
    fun leg1OnChain_processingTransaction_doesNotComplete() = runTest(dispatcher) {
        val swap = swap(
            leg1IsOffChain = false,
            leg1TransactionId = null,
            leg1AmountOut = BigDecimal("5.0")
        )

        val outputSource = mockk<TransactionSource>()
        val outputWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "the-open-network"
            every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
            every { transactionSource } returns outputSource
        }
        val txAdapter = mockk<ITransactionsAdapter>(relaxed = true) {
            every { lastBlockInfo } returns LastBlockInfo(100)
        }
        val mainValue = mockk<TransactionValue>(relaxed = true) {
            every { decimalValue } returns BigDecimal("4.8")
        }
        val processingRecord = mockk<TransactionRecord>(relaxed = true) {
            every { uid } returns "processing-tx"
            every { timestamp } returns (swap.createdAt / 1000) + 10
            every { this@mockk.mainValue } returns mainValue
            every { failed } returns false
            every { status(any()) } returns TransactionStatus.Processing(0.5f)
        }

        every { walletManager.activeWallets } returns listOf(outputWallet)
        every { transactionAdapterManager.getAdapter(outputSource) } returns txAdapter
        coEvery {
            txAdapter.getTransactions(any(), any(), any(), eq(FilterTransactionType.All), any())
        } returns listOf(processingRecord)
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)

        useCase()

        coVerify(exactly = 0) {
            pendingMultiSwapStorage.updateLeg1(any(), any(), any(), any())
        }
    }

    @Test
    fun leg1OnChain_completedTransaction_marksCompleted() = runTest(dispatcher) {
        val swap = swap(
            leg1IsOffChain = false,
            leg1TransactionId = null,
            leg1AmountOut = BigDecimal("5.0")
        )

        val outputSource = mockk<TransactionSource>()
        val outputWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "the-open-network"
            every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
            every { transactionSource } returns outputSource
        }
        val txAdapter = mockk<ITransactionsAdapter>(relaxed = true) {
            every { lastBlockInfo } returns LastBlockInfo(100)
        }
        val mainValue = mockk<TransactionValue>(relaxed = true) {
            every { decimalValue } returns BigDecimal("4.8")
        }
        val completedRecord = mockk<TransactionRecord>(relaxed = true) {
            every { uid } returns "completed-tx"
            every { timestamp } returns (swap.createdAt / 1000) + 10
            every { this@mockk.mainValue } returns mainValue
            every { failed } returns false
            every { status(any()) } returns TransactionStatus.Completed
        }

        every { walletManager.activeWallets } returns listOf(outputWallet)
        every { transactionAdapterManager.getAdapter(outputSource) } returns txAdapter
        coEvery {
            txAdapter.getTransactions(any(), any(), any(), eq(FilterTransactionType.All), any())
        } returns listOf(completedRecord)
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg1Status = PendingMultiSwap.STATUS_COMPLETED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("4.8"),
                transactionId = null,
            )
        }
    }

    // --- extractReceivedAmount for different record types ---

    @Test
    fun leg1OnChain_tonSwapRecord_extractsAmountFromValueOut() = runTest(dispatcher) {
        val swap = swap(
            leg1IsOffChain = false,
            leg1TransactionId = null,
            leg1AmountOut = BigDecimal("3.93")
        )

        val outputSource = mockk<TransactionSource>()
        val outputWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "the-open-network"
            every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
            every { transactionSource } returns outputSource
        }
        val txAdapter = mockk<ITransactionsAdapter>(relaxed = true) {
            every { lastBlockInfo } returns null
        }

        val valueOut = mockk<TransactionValue>(relaxed = true) {
            every { decimalValue } returns BigDecimal("3.93")
        }
        val swapAction = TonTransactionRecord.Action(
            type = TonTransactionRecord.Action.Type.Swap(
                routerName = "STON.fi DEX",
                routerAddress = "EQaddr",
                valueIn = mockk(relaxed = true),
                valueOut = valueOut,
            ),
            status = TransactionStatus.Completed,
        )
        val tonRecord = mockk<TonTransactionRecord>(relaxed = true) {
            every { uid } returns "ton-swap-uid"
            every { timestamp } returns (swap.createdAt / 1000) + 5
            every { mainValue } returns null
            every { actions } returns listOf(swapAction)
            every { failed } returns false
            every { status(any()) } returns TransactionStatus.Completed
        }

        every { walletManager.activeWallets } returns listOf(outputWallet)
        every { transactionAdapterManager.getAdapter(outputSource) } returns txAdapter
        coEvery {
            txAdapter.getTransactions(any(), any(), any(), eq(FilterTransactionType.All), any())
        } returns listOf(tonRecord)
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg1Status = PendingMultiSwap.STATUS_COMPLETED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("3.93"),
                transactionId = null,
            )
        }
    }

    @Test
    fun leg1OnChain_tonSwapRecord_mainValueNull_doesNotSkipCandidate() = runTest(dispatcher) {
        val swap = swap(
            leg1IsOffChain = false,
            leg1TransactionId = null,
            leg1AmountOut = BigDecimal("3.93")
        )

        val outputSource = mockk<TransactionSource>()
        val outputWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "the-open-network"
            every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
            every { transactionSource } returns outputSource
        }
        val txAdapter = mockk<ITransactionsAdapter>(relaxed = true) {
            every { lastBlockInfo } returns null
        }

        // TON swap record where mainValue is null (as in production)
        val valueOut = mockk<TransactionValue>(relaxed = true) {
            every { decimalValue } returns BigDecimal("3.93")
        }
        val swapAction = TonTransactionRecord.Action(
            type = TonTransactionRecord.Action.Type.Swap(
                routerName = "STON.fi DEX",
                routerAddress = "EQaddr",
                valueIn = mockk(relaxed = true),
                valueOut = valueOut,
            ),
            status = TransactionStatus.Completed,
        )
        val tonRecord = mockk<TonTransactionRecord>(relaxed = true) {
            every { uid } returns "ton-swap-uid"
            every { timestamp } returns (swap.createdAt / 1000) + 5
            every { mainValue } returns null // mainValue is null for swap actions
            every { actions } returns listOf(swapAction)
            every { failed } returns false
            every { status(any()) } returns TransactionStatus.Completed
        }

        every { walletManager.activeWallets } returns listOf(outputWallet)
        every { transactionAdapterManager.getAdapter(outputSource) } returns txAdapter
        coEvery {
            txAdapter.getTransactions(any(), any(), any(), eq(FilterTransactionType.All), any())
        } returns listOf(tonRecord)
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg1Status = PendingMultiSwap.STATUS_COMPLETED)

        useCase()

        // Should NOT skip the candidate despite mainValue == null
        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("3.93"),
                transactionId = null,
            )
        }
    }

    @Test
    fun leg1OnChain_evmSwapRecord_extractsAmountFromValueOut() = runTest(dispatcher) {
        val swap = swap(
            leg1IsOffChain = false,
            leg1TransactionId = null,
            leg1AmountOut = BigDecimal("10.0")
        )

        val outputSource = mockk<TransactionSource>()
        val outputWallet = mockk<Wallet>(relaxed = true) {
            every { coin.uid } returns "the-open-network"
            every { token.blockchainType } returns BlockchainType.fromUid("the-open-network")
            every { transactionSource } returns outputSource
        }
        val txAdapter = mockk<ITransactionsAdapter>(relaxed = true) {
            every { lastBlockInfo } returns LastBlockInfo(100)
        }

        val valueOut = mockk<TransactionValue>(relaxed = true) {
            every { decimalValue } returns BigDecimal("9.8")
        }
        val evmRecord = mockk<EvmTransactionRecord>(relaxed = true) {
            every { uid } returns "evm-swap-uid"
            every { timestamp } returns (swap.createdAt / 1000) + 5
            every { mainValue } returns null // mainValue can be null for EVM swaps
            every { this@mockk.valueOut } returns valueOut
            every { failed } returns false
            every { status(any()) } returns TransactionStatus.Completed
        }

        every { walletManager.activeWallets } returns listOf(outputWallet)
        every { transactionAdapterManager.getAdapter(outputSource) } returns txAdapter
        coEvery {
            txAdapter.getTransactions(any(), any(), any(), eq(FilterTransactionType.All), any())
        } returns listOf(evmRecord)
        coEvery { pendingMultiSwapStorage.getAllOnceByAccountId("test-account") } returns listOf(swap)
        coEvery { pendingMultiSwapStorage.getById("swap-1") } returns swap.copy(leg1Status = PendingMultiSwap.STATUS_COMPLETED)

        useCase()

        coVerify {
            pendingMultiSwapStorage.updateLeg1(
                id = "swap-1",
                status = PendingMultiSwap.STATUS_COMPLETED,
                amountOut = BigDecimal("9.8"),
                transactionId = null,
            )
        }
    }
}
