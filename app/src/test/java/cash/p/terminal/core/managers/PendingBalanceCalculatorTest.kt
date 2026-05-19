package cash.p.terminal.core.managers

import cash.p.terminal.entities.PendingTransactionEntity
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class PendingBalanceCalculatorTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var pendingRepository: PendingTransactionRepository
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var pendingFlow: MutableStateFlow<List<PendingTransactionEntity>>

    @Before
    fun setUp() {
        pendingRepository = mockk(relaxed = true)
        pendingFlow = MutableStateFlow(emptyList())

        every { pendingRepository.getActivePendingFlow(any()) } returns pendingFlow

        dispatcherProvider = object : DispatcherProvider {
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
            override val main: CoroutineDispatcher = dispatcher
            override val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
        }
    }

    @Test
    fun pendingChangedFlow_emitsWhenPendingTransactionAdded() = runTest(dispatcher) {
        // Given: PendingBalanceCalculator is observing an account
        val calculator = PendingBalanceCalculator(pendingRepository, dispatcherProvider)
        calculator.startObserving("account-1")
        advanceUntilIdle()

        var emitted = false
        val job = launch {
            calculator.pendingChangedFlow.first()
            emitted = true
        }

        // When: A pending transaction is added
        pendingFlow.value = listOf(createPendingEntity("tx-1"))
        advanceUntilIdle()

        // Then: pendingChangedFlow should emit
        job.cancel()
        assertTrue("pendingChangedFlow should emit when pending transaction is added", emitted)
    }

    @Test
    fun pendingChangedFlow_emitsWhenPendingTransactionRemoved() = runTest(dispatcher) {
        // Given: PendingBalanceCalculator has existing pending transactions
        pendingFlow.value = listOf(createPendingEntity("tx-1"))
        val calculator = PendingBalanceCalculator(pendingRepository, dispatcherProvider)
        calculator.startObserving("account-1")
        advanceUntilIdle()

        var emitted = false
        val job = launch {
            calculator.pendingChangedFlow.first()
            emitted = true
        }

        // When: Pending transaction is removed (confirmed)
        pendingFlow.value = emptyList()
        advanceUntilIdle()

        // Then: pendingChangedFlow should emit
        job.cancel()
        assertTrue("pendingChangedFlow should emit when pending transaction is removed", emitted)
    }

    @Test
    fun onPendingInserted_updatesCache_adjustBalanceReflectsDeduction() = runTest(dispatcher) {
        val calculator = PendingBalanceCalculator(pendingRepository, dispatcherProvider)
        val entity = createPendingEntity("tx-sync", blockchainTypeUid = "the-open-network")

        // When: entity inserted synchronously (no Room Flow)
        calculator.onPendingInserted(entity)

        // Then: adjustBalance should reflect the deduction immediately
        val wallet = createMockWallet()
        // sdkBalance = 87.3657 TON (same as sdkBalanceAtCreation → SDK hasn't deducted yet)
        val rawBalance = BalanceData(available = BigDecimal("87.3657"))
        val adjusted = calculator.adjustBalance(wallet, rawBalance)

        // Expected deduction: amount(80) + fee(0.1) = 80.1 TON
        val expectedBalance = BigDecimal("87.3657") - BigDecimal("80.1")
        assertEquals(
            expectedBalance.stripTrailingZeros(),
            adjusted.available.stripTrailingZeros()
        )
    }

    @Test
    fun onPendingInserted_emitsPendingChangedFlow() = runTest(dispatcher) {
        val calculator = PendingBalanceCalculator(pendingRepository, dispatcherProvider)

        var emitted = false
        val job = launch {
            calculator.pendingChangedFlow.first()
            emitted = true
        }

        // When: entity inserted synchronously
        calculator.onPendingInserted(createPendingEntity("tx-emit"))
        advanceUntilIdle()

        // Then: pendingChangedFlow should emit
        job.cancel()
        assertTrue("pendingChangedFlow should emit on onPendingInserted", emitted)
    }

    // For Bitcoin/MWEB the SDK deducts spent UTXOs on broadcast: currentSdkBalance
    // immediately equals sdkBalanceAtCreation - amount - fee. The cleanup heuristic
    // matches "currentSdkBalance is close to expectedAfterConfirm" and
    // fires the moment the broadcast completes, dropping the pending entity before
    // the transaction is actually included in a block. The expected behaviour is to
    // keep the pending entry until the SDK reports the confirmed change output.
    @Test
    fun adjustBalance_mwebSdkDeductedImmediately_doesNotDeletePendingPrematurely() = runTest(dispatcher) {
        val calculator = PendingBalanceCalculator(pendingRepository, dispatcherProvider)
        calculator.onPendingInserted(
            createMwebLitecoinPendingEntity(
                id = "ltc-mweb-pending",
                amountAtomic = "1000000",
                feeAtomic = "100000",
                sdkBalanceAtCreationAtomic = "2600000"
            )
        )

        val wallet = createMwebLitecoinWallet()
        // SDK already deducted on broadcast: 0.026 - 0.01 - 0.001 = 0.015 LTC.
        val rawBalance = BalanceData(available = BigDecimal("0.015"))

        calculator.adjustBalance(wallet, rawBalance)
        advanceUntilIdle()

        coVerify(exactly = 0) { pendingRepository.deleteByIds(any()) }
    }

    @Test
    fun adjustBalance_mwebSdkTemporarilyReportsZeroBeforeChangeSnapshot_returnsExpectedRemainingBalance() = runTest(dispatcher) {
        val calculator = PendingBalanceCalculator(pendingRepository, dispatcherProvider)
        calculator.onPendingInserted(
            createMwebLitecoinPendingEntity(
                id = "ltc-mweb-pending",
                amountAtomic = "1000000",
                feeAtomic = "100000",
                sdkBalanceAtCreationAtomic = "2600000"
            )
        )

        val wallet = createMwebLitecoinWallet()
        val rawBalance = BalanceData(available = BigDecimal.ZERO)

        val adjusted = calculator.adjustBalance(wallet, rawBalance)
        advanceUntilIdle()

        assertEquals(
            BigDecimal("0.015").stripTrailingZeros(),
            adjusted.available.stripTrailingZeros()
        )
        coVerify(exactly = 0) { pendingRepository.deleteByIds(any()) }
    }

    @Test
    fun adjustBalance_mwebSdkReportsPositiveBelowExpected_keepsSdkAvailableBalance() = runTest(dispatcher) {
        val calculator = PendingBalanceCalculator(pendingRepository, dispatcherProvider)
        calculator.onPendingInserted(
            createMwebLitecoinPendingEntity(
                id = "ltc-mweb-pending",
                amountAtomic = "1000000",
                feeAtomic = "100000",
                sdkBalanceAtCreationAtomic = "2600000"
            )
        )

        val wallet = createMwebLitecoinWallet()
        val rawBalance = BalanceData(available = BigDecimal("0.014"))

        val adjusted = calculator.adjustBalance(wallet, rawBalance)

        assertEquals(
            BigDecimal("0.014").stripTrailingZeros(),
            adjusted.available.stripTrailingZeros()
        )
    }

    @Test
    fun adjustBalance_publicLitecoinSdkDeductedImmediately_returnsSdkAvailableBalance() = runTest(dispatcher) {
        val tokenType = TokenType.Derived(TokenType.Derivation.Bip84)
        val calculator = PendingBalanceCalculator(pendingRepository, dispatcherProvider)
        calculator.onPendingInserted(
            createLitecoinPendingEntity(
                id = "ltc-public-pending",
                tokenTypeId = tokenType.id,
                amountAtomic = "1000000",
                feeAtomic = "100000",
                sdkBalanceAtCreationAtomic = "2600000"
            )
        )

        val wallet = createLitecoinWallet(tokenType)
        val rawBalance = BalanceData(available = BigDecimal("0.015"))

        val adjusted = calculator.adjustBalance(wallet, rawBalance)

        assertEquals(
            BigDecimal("0.015").stripTrailingZeros(),
            adjusted.available.stripTrailingZeros()
        )
    }

    private fun createMockWallet(): Wallet {
        val account = mockk<Account> {
            every { id } returns "account-1"
        }
        val token = mockk<Token> {
            every { coin } returns Coin(uid = "toncoin", name = "Toncoin", code = "TON")
            every { type } returns TokenType.Native
            every { blockchainType } returns BlockchainType.Ton
            every { decimals } returns 9
        }
        return mockk {
            every { this@mockk.account } returns account
            every { this@mockk.token } returns token
        }
    }

    private fun createMwebLitecoinWallet(): Wallet {
        return createLitecoinWallet(TokenType.Mweb)
    }

    private fun createLitecoinWallet(tokenType: TokenType): Wallet {
        val account = mockk<Account> {
            every { id } returns "account-1"
        }
        val token = mockk<Token> {
            every { coin } returns Coin(uid = "litecoin", name = "Litecoin", code = "LTC")
            every { type } returns tokenType
            every { blockchainType } returns BlockchainType.Litecoin
            every { decimals } returns 8
        }
        return mockk {
            every { this@mockk.account } returns account
            every { this@mockk.token } returns token
        }
    }

    private fun createMwebLitecoinPendingEntity(
        id: String,
        amountAtomic: String,
        feeAtomic: String,
        sdkBalanceAtCreationAtomic: String
    ) = createLitecoinPendingEntity(
        id = id,
        tokenTypeId = TokenType.Mweb.id,
        amountAtomic = amountAtomic,
        feeAtomic = feeAtomic,
        sdkBalanceAtCreationAtomic = sdkBalanceAtCreationAtomic,
    )

    private fun createLitecoinPendingEntity(
        id: String,
        tokenTypeId: String,
        amountAtomic: String,
        feeAtomic: String,
        sdkBalanceAtCreationAtomic: String
    ) = PendingTransactionEntity(
        id = id,
        walletId = "account-1",
        coinUid = "litecoin",
        blockchainTypeUid = "litecoin",
        tokenTypeId = tokenTypeId,
        meta = null,
        amountAtomic = amountAtomic,
        feeAtomic = feeAtomic,
        sdkBalanceAtCreationAtomic = sdkBalanceAtCreationAtomic,
        fromAddress = "",
        toAddress = "ltcmweb...",
        txHash = null,
        nonce = null,
        memo = null,
        createdAt = System.currentTimeMillis(),
        expiresAt = System.currentTimeMillis() + 3600000
    )

    private fun createPendingEntity(
        id: String,
        blockchainTypeUid: String = "ton"
    ) = PendingTransactionEntity(
        id = id,
        walletId = "account-1",
        coinUid = "toncoin",
        blockchainTypeUid = blockchainTypeUid,
        tokenTypeId = "native",
        meta = null,
        amountAtomic = "80000000000",
        feeAtomic = "100000000",
        sdkBalanceAtCreationAtomic = "87365700000",
        fromAddress = "",
        toAddress = "EQC...",
        txHash = null,
        nonce = null,
        memo = null,
        createdAt = System.currentTimeMillis(),
        expiresAt = System.currentTimeMillis() + 3600000
    )
}
