package cash.p.terminal.modules.transactionInfo

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.managers.AmlStatusManager
import cash.p.terminal.core.managers.PendingTransactionMatcher
import cash.p.terminal.core.managers.PoisonAddressManager
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.core.usecase.UpdateSwapProviderTransactionsStatusUseCase
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.PendingTransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.modules.transactions.NftMetadataService
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.network.changenow.domain.entity.TransactionStatusEnum
import cash.p.terminal.network.swaprepository.SwapProvider
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.CurrencyManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.reactivex.subjects.PublishSubject
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private val dispatcher = UnconfinedTestDispatcher()

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
    private val pendingTransactionMatcher = spyk(PendingTransactionMatcher())
    private val amlStatusManager = mockk<AmlStatusManager>(relaxed = true)
    private lateinit var dispatcherProvider: DispatcherProvider

    private val lastBlockSubject = PublishSubject.create<Unit>()

    @get:Rule
    val koinRule = KoinTestRule.create {
        modules(
            module {
                single<IBalanceHiddenManager> { balanceHiddenManager }
                single { pendingTransactionMatcher }
                single { amlStatusManager }
                single { mockk<PoisonAddressManager>(relaxed = true) }
            }
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dispatcherProvider = TestDispatcherProvider(dispatcher, CoroutineScope(dispatcher))

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
        initialTransactionRecord: TransactionRecord = transactionRecord,
        userSwapTransactionId: String? = null
    ) = TransactionInfoService(
        initialTransactionRecord = initialTransactionRecord,
        userSwapTransactionId = userSwapTransactionId,
        walletUid = "wallet-1",
        adapter = adapter,
        marketKit = marketKit,
        currencyManager = currencyManager,
        nftMetadataService = nftMetadataService,
        updateSwapProviderTransactionsStatusUseCase = updateSwapProviderTransactionsStatusUseCase,
        swapProviderTransactionsStorage = swapProviderTransactionsStorage,
        dispatcherProvider = dispatcherProvider,
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

    @Test
    fun start_pendingIncomingLikeRecord_doesNotReplacePendingRecord() = runTest(dispatcher) {
        val token = createToken()
        val source = createSource(blockchain = token.blockchain)
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-1",
            transactionHash = "",
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00000563"),
            toAddress = "bc1-recipient-address",
        )
        val incomingRecord = createMockRecord(
            recordUid = "incoming-real",
            recordHash = "incoming-hash",
            source = source,
            recordToken = token,
            mainValue = TransactionValue.CoinValue(token, BigDecimal("0.00000563")),
            timestamp = 1_715_000_005,
            toAddress = "bc1-wallet-address",
            type = TransactionRecordType.BITCOIN_INCOMING,
        )
        every { adapter.getTransactionRecordsFlow(null, cash.p.terminal.modules.transactions.FilterTransactionType.All, null) } returns
            MutableStateFlow(listOf(incomingRecord))

        val service = createService(initialTransactionRecord = pendingRecord)
        backgroundScope.launch { service.start() }
        advanceUntilIdle()

        assertEquals(pendingRecord.uid, service.transactionRecord.uid)
    }

    @Test
    fun start_pendingSameAddressSameAmountButOldReal_doesNotReplacePendingRecord() = runTest(dispatcher) {
        val token = createToken()
        val source = createSource(blockchain = token.blockchain)
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-1",
            transactionHash = "",
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00000563"),
            toAddress = "bc1same-address",
        )
        val oldRealRecord = createMockRecord(
            recordUid = "old-real",
            recordHash = "old-real-hash",
            source = source,
            recordToken = token,
            mainValue = TransactionValue.CoinValue(token, BigDecimal("-0.00000563")),
            timestamp = 1_700_000_000,
            toAddress = "bc1same-address",
            type = TransactionRecordType.BITCOIN_OUTGOING,
        )
        every { adapter.getTransactionRecordsFlow(null, cash.p.terminal.modules.transactions.FilterTransactionType.All, null) } returns
            MutableStateFlow(listOf(oldRealRecord))

        val service = createService(initialTransactionRecord = pendingRecord)
        backgroundScope.launch { service.start() }
        advanceUntilIdle()

        assertEquals(pendingRecord.uid, service.transactionRecord.uid)
    }

    @Test
    fun start_pendingTokenTransfer_realOutgoingWithBaseToken_replacesPendingRecord() = runTest(dispatcher) {
        val blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null)
        val source = createSource(blockchain = blockchain)
        val baseToken = createEvmToken(
            coinUid = "ethereum",
            coinName = "Ethereum",
            coinCode = "ETH",
            blockchain = blockchain,
            type = TokenType.Native,
            decimals = 18,
        )
        val assetToken = createEvmToken(
            coinUid = "usdc",
            coinName = "USD Coin",
            coinCode = "USDC",
            blockchain = blockchain,
            type = TokenType.Eip20("0xusdc"),
            decimals = 6,
        )
        val pendingRecord = createPendingRecord(
            token = assetToken,
            source = source,
            uid = "pending-1",
            transactionHash = "",
            timestamp = 1_715_000_000,
            amount = BigDecimal("12.34"),
            toAddress = "0xrecipient",
        )
        val realRecord = createMockRecord(
            recordUid = "real-outgoing",
            recordHash = "real-hash",
            source = source,
            recordToken = baseToken,
            mainValue = TransactionValue.CoinValue(assetToken, BigDecimal("-12.34")),
            timestamp = 1_715_000_005,
            toAddress = "0xrecipient",
            type = TransactionRecordType.EVM_OUTGOING,
        )
        every { adapter.getTransactionRecordsFlow(null, cash.p.terminal.modules.transactions.FilterTransactionType.All, null) } returns
            MutableStateFlow(listOf(realRecord))

        val service = createService(initialTransactionRecord = pendingRecord)
        backgroundScope.launch { service.start() }
        advanceUntilIdle()

        assertEquals(realRecord.uid, service.transactionRecord.uid)
    }

    @Test
    fun start_pendingSwap_realEvmSwapWithBaseToken_replacesPendingRecord() = runTest(dispatcher) {
        val blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null)
        val source = createSource(blockchain = blockchain)
        val baseToken = createEvmToken(
            coinUid = "ethereum",
            coinName = "Ethereum",
            coinCode = "ETH",
            blockchain = blockchain,
            type = TokenType.Native,
            decimals = 18,
        )
        val assetToken = createEvmToken(
            coinUid = "usdc",
            coinName = "USD Coin",
            coinCode = "USDC",
            blockchain = blockchain,
            type = TokenType.Eip20("0xusdc"),
            decimals = 6,
        )
        val pendingRecord = createPendingRecord(
            token = assetToken,
            source = source,
            uid = "pending-swap",
            transactionHash = "",
            timestamp = 1_715_000_000,
            amount = BigDecimal("12.34"),
            toAddress = "0xrouter",
        )
        val realRecord = createMockEvmSwapRecord(
            recordUid = "real-swap",
            recordHash = "real-swap-hash",
            source = source,
            recordToken = baseToken,
            valueIn = TransactionValue.CoinValue(assetToken, BigDecimal("-12.34")),
            timestamp = 1_715_000_005,
            toAddress = "0xrouter",
        )
        every { adapter.getTransactionRecordsFlow(null, cash.p.terminal.modules.transactions.FilterTransactionType.All, null) } returns
            MutableStateFlow(listOf(realRecord))

        val service = createService(initialTransactionRecord = pendingRecord)
        backgroundScope.launch { service.start() }
        advanceUntilIdle()

        assertEquals(realRecord.uid, service.transactionRecord.uid)
    }

    private fun createPendingRecord(
        token: Token,
        source: TransactionSource,
        uid: String,
        transactionHash: String,
        timestamp: Long,
        amount: BigDecimal,
        toAddress: String,
    ) = PendingTransactionRecord(
        uid = uid,
        transactionHash = transactionHash,
        timestamp = timestamp,
        source = source,
        token = token,
        amount = amount,
        toAddress = toAddress,
        fromAddress = "",
        expiresAt = Long.MAX_VALUE,
        memo = null,
    )

    private fun createMockRecord(
        recordUid: String,
        recordHash: String,
        source: TransactionSource,
        recordToken: Token,
        mainValue: TransactionValue,
        timestamp: Long,
        toAddress: String,
        type: TransactionRecordType,
    ): TransactionRecord = mockk {
        every { uid } returns recordUid
        every { transactionHash } returns recordHash
        every { this@mockk.source } returns source
        every { blockchainType } returns source.blockchain.type
        every { token } returns recordToken
        every { this@mockk.mainValue } returns mainValue
        every { this@mockk.timestamp } returns timestamp
        every { to } returns listOf(toAddress)
        every { from } returns null
        every { transactionRecordType } returns type
    }

    private fun createMockEvmSwapRecord(
        recordUid: String,
        recordHash: String,
        source: TransactionSource,
        recordToken: Token,
        valueIn: TransactionValue,
        timestamp: Long,
        toAddress: String,
    ): TransactionRecord = mockk<EvmTransactionRecord> {
        every { uid } returns recordUid
        every { transactionHash } returns recordHash
        every { this@mockk.source } returns source
        every { blockchainType } returns source.blockchain.type
        every { token } returns recordToken
        every { this@mockk.mainValue } returns null
        every { this@mockk.valueIn } returns valueIn
        every { this@mockk.timestamp } returns timestamp
        every { to } returns listOf(toAddress)
        every { from } returns null
        every { transactionRecordType } returns TransactionRecordType.EVM_SWAP
    }

    private fun createToken(): Token {
        val coin = Coin(uid = "bitcoin", name = "Bitcoin", code = "BTC")
        val blockchain = Blockchain(BlockchainType.Bitcoin, "Bitcoin", null)
        return Token(
            coin = coin,
            blockchain = blockchain,
            type = TokenType.Derived(TokenType.Derivation.Bip86),
            decimals = 8,
        )
    }

    private fun createEvmToken(
        coinUid: String,
        coinName: String,
        coinCode: String,
        blockchain: Blockchain,
        type: TokenType,
        decimals: Int,
    ) = Token(
        coin = Coin(uid = coinUid, name = coinName, code = coinCode),
        blockchain = blockchain,
        type = type,
        decimals = decimals,
    )

    private fun createSource(blockchain: Blockchain): TransactionSource {
        val account = Account(
            id = "account-1",
            name = "Main",
            type = mockk(relaxed = true),
            origin = AccountOrigin.Created,
            level = 0,
        )
        return TransactionSource(blockchain = blockchain, account = account, meta = null)
    }
}
