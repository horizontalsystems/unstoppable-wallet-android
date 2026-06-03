package cash.p.terminal.core.adapters.zcash

import android.content.Context
import android.database.sqlite.SQLiteDatabaseCorruptException
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.managers.BackgroundKeepAliveManager
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.domain.usecase.ClearZCashWalletDataUseCase
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Wallet
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException
import cash.z.ecc.android.sdk.model.AccountBalance
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.PercentDecimal
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.ZcashNetwork
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.CoreApp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Tests for ZcashAdapter database corruption detection and recovery.
 *
 * Recovery runs on Dispatchers.IO, so tests wait for recovery to reach resubscription
 * before tearDown resets Dispatchers.Main.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ZcashAdapterCorruptionRecoveryTest {

    companion object {
        private const val VERIFY_TIMEOUT = 5000L
        private const val RETRY_DELAY_GUARD_MS = 4000L
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private val testScope = CoroutineScope(dispatcher + SupervisorJob())

    private val context = mockk<Context>(relaxed = true)
    private val wallet = mockk<Wallet>(relaxed = true)
    private val localStorage = mockk<ILocalStorage>(relaxed = true)
    private val backgroundManager = mockk<BackgroundManager>(relaxed = true)
    private val singleUseAddressManager = mockk<ZcashSingleUseAddressManager>(relaxed = true)
    private val clearZCashWalletDataUseCase = mockk<ClearZCashWalletDataUseCase>(relaxed = true)
    private val restoreSettings = RestoreSettings().apply { birthdayHeight = 2000000L }

    private lateinit var mockSynchronizer: SdkSynchronizer

    private val statusFlow = MutableStateFlow(Synchronizer.Status.SYNCING)
    private val progressFlow = MutableStateFlow(PercentDecimal.ZERO_PERCENT)
    private val walletBalancesFlow = MutableStateFlow<Map<AccountUuid, AccountBalance>?>(null)
    private val processorInfoFlow = MutableStateFlow(
        CompactBlockProcessor.ProcessorInfo(null, null, null)
    )
    private val allTransactionsFlow = MutableStateFlow<List<TransactionOverview>>(emptyList())

    private var capturedProcessorErrorHandler: ((Throwable?) -> Boolean)? = null
    private var capturedCriticalErrorHandler: ((Throwable?) -> Boolean)? = null

    private lateinit var adapter: ZcashAdapter

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        CoreApp.instance = mockk(relaxed = true)

        startKoin {
            modules(module {
                single { clearZCashWalletDataUseCase }
                single { mockk<BackgroundKeepAliveManager>(relaxed = true) }
            })
        }

        val testSeed = ByteArray(64) { it.toByte() }
        val accountType = mockk<AccountType.Mnemonic>(relaxed = true) {
            every { seed } returns testSeed
        }
        val account = mockk<Account>(relaxed = true) {
            every { id } returns "test-account-id"
            every { name } returns "Test"
            every { type } returns accountType
            every { origin } returns AccountOrigin.Created
        }
        every { wallet.account } returns account
        every { localStorage.zcashAccountIds } returns setOf("test-account-id")
        every { localStorage.torEnabled } returns false
        every { backgroundManager.stateFlow } returns MutableStateFlow(BackgroundManagerState.Unknown)
        every { clearZCashWalletDataUseCase.getValidAliasFromAccountId(any(), any()) } returns "zcash_test"

        mockkObject(BlockHeight.Companion)
        coEvery { BlockHeight.ofLatestCheckpoint(any(), any()) } returns BlockHeight.new(2500000L)

        setupMockSynchronizer()
        mockSynchronizerCompanion()
    }

    private fun setupMockSynchronizer() {
        mockSynchronizer = mockk<SdkSynchronizer>(relaxed = true) {
            every { status } returns statusFlow
            every { progress } returns progressFlow
            every { walletBalances } returns walletBalancesFlow
            every { processorInfo } returns processorInfoFlow
            every { allTransactions } returns allTransactionsFlow
            every { coroutineScope } returns testScope
            every { latestHeight } returns null
        }

        val processorSlot = slot<(Throwable?) -> Boolean>()
        every { mockSynchronizer.onProcessorErrorHandler = capture(processorSlot) } answers {
            capturedProcessorErrorHandler = processorSlot.captured
        }
        val criticalSlot = slot<(Throwable?) -> Boolean>()
        every { mockSynchronizer.onCriticalErrorHandler = capture(criticalSlot) } answers {
            capturedCriticalErrorHandler = criticalSlot.captured
        }
    }

    private fun mockSynchronizerCompanion() {
        mockkObject(Synchronizer)
        coEvery { Synchronizer.erase(any(), any(), any()) } returns true
        every {
            Synchronizer.newBlocking(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(), walletInitMode = any(),
                setup = any(), isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        } returns mockSynchronizer

        coEvery {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(), walletInitMode = any(),
                setup = any(), isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        } returns mockSynchronizer
    }

    private fun createAdapter(): ZcashAdapter {
        return ZcashAdapter(
            context = context,
            wallet = wallet,
            restoreSettings = restoreSettings,
            addressSpecTyped = null,
            localStorage = localStorage,
            backgroundManager = backgroundManager,
            singleUseAddressManager = singleUseAddressManager,
            dispatcherProvider = TestDispatcherProvider(dispatcher, testScope),
        )
    }

    @After
    fun tearDown() {
        if (::adapter.isInitialized) {
            adapter.stop()
        }
        testScope.cancel()
        stopKoin()
        Dispatchers.resetMain()
        unmockkAll()
    }

    // --- onProcessorErrorHandler ---

    @Test
    fun onProcessorError_corruptionDetected_triggersRecovery() = runTest(dispatcher) {
        adapter = createAdapter()

        val result = capturedProcessorErrorHandler?.invoke(
            SQLiteDatabaseCorruptException("database disk image is malformed")
        )

        assertFalse("Should return false to signal abort", result ?: true)
        coVerify(timeout = VERIFY_TIMEOUT) { Synchronizer.erase(any(), ZcashNetwork.Mainnet, "zcash_test") }
        verifyRecoveryResubscribed()
    }

    @Test
    fun onProcessorError_rustDatabaseMalformed_triggersRecovery() = runTest(dispatcher) {
        adapter = createAdapter()

        val rustError = RuntimeException(
            "Rust error while scanning blocks (limit 10): " +
                    "The underlying datasource produced the following error: " +
                    "database disk image is malformed"
        )
        val result = capturedProcessorErrorHandler?.invoke(
            CompactBlockProcessorException.FailedSynchronizationException(
                "unable to resolve the error after 5 correction attempts",
                CompactBlockProcessorException.FailedScanException(rustError),
            )
        )

        assertFalse("Should detect Rust database malformed error", result ?: true)
        coVerify(timeout = VERIFY_TIMEOUT) { Synchronizer.erase(any(), ZcashNetwork.Mainnet, "zcash_test") }
        verifyRecoveryResubscribed()
    }

    @Test
    fun onProcessorError_nonCorruptionError_doesNotTriggerRecovery() = runTest(dispatcher) {
        adapter = createAdapter()

        val result = capturedProcessorErrorHandler?.invoke(RuntimeException("some other error"))

        assertTrue("Should return true to signal retry", result ?: false)
        coVerify(exactly = 0, timeout = VERIFY_TIMEOUT) { Synchronizer.erase(any(), any(), any()) }
    }

    // --- onCriticalErrorHandler ---

    @Test
    fun onCriticalError_corruptionDetected_triggersRecovery() = runTest(dispatcher) {
        adapter = createAdapter()

        val result = capturedCriticalErrorHandler?.invoke(
            SQLiteDatabaseCorruptException("database disk image is malformed")
        )

        assertFalse("Should return false to signal abort", result ?: true)
        coVerify(timeout = VERIFY_TIMEOUT) { Synchronizer.erase(any(), ZcashNetwork.Mainnet, "zcash_test") }
        verifyRecoveryResubscribed()
    }

    @Test
    fun onCriticalError_wrappedCorruption_triggersRecovery() = runTest(dispatcher) {
        adapter = createAdapter()

        val corruption = SQLiteDatabaseCorruptException("database disk image is malformed")
        val wrapped = RuntimeException("flow failed", corruption)
        val result = capturedCriticalErrorHandler?.invoke(wrapped)

        assertFalse("Should detect wrapped corruption", result ?: true)
        coVerify(timeout = VERIFY_TIMEOUT) { Synchronizer.erase(any(), ZcashNetwork.Mainnet, "zcash_test") }
        verifyRecoveryResubscribed()
    }

    // --- Flow-level catch ---

    @Test
    fun flowCorruption_triggersRecovery() = runTest(dispatcher) {
        val corruptFlow = flow<List<TransactionOverview>> {
            throw SQLiteDatabaseCorruptException("database disk image is malformed")
        }
        every { mockSynchronizer.allTransactions } returns corruptFlow

        adapter = createAdapter()
        adapter.start()

        coVerify(timeout = VERIFY_TIMEOUT) { Synchronizer.erase(any(), ZcashNetwork.Mainnet, "zcash_test") }
        verifySynchronizerNew()
        verify(timeout = VERIFY_TIMEOUT, atLeast = 2) { mockSynchronizer.processorInfo }
    }

    // --- Recovery correctness ---

    @Test
    fun recovery_usesRestoreWalletMode() = runTest(dispatcher) {
        var capturedInitMode: WalletInitMode? = null
        coEvery {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(),
                walletInitMode = any(), setup = any(),
                isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        } answers {
            for (a in args) {
                if (a is WalletInitMode) capturedInitMode = a
            }
            mockSynchronizer
        }

        adapter = createAdapter()

        capturedProcessorErrorHandler?.invoke(
            SQLiteDatabaseCorruptException("database disk image is malformed")
        )

        coVerify(timeout = VERIFY_TIMEOUT) {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(),
                walletInitMode = any(), setup = any(),
                isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        }
        assertEquals(WalletInitMode.RestoreWallet, capturedInitMode)
        verify(timeout = VERIFY_TIMEOUT, atLeast = 1) { mockSynchronizer.processorInfo }
    }

    @Test
    fun recovery_retryAfterFailedNew_preservesRestoreWalletMode() = runTest(dispatcher) {
        val capturedModes = mutableListOf<WalletInitMode>()
        var newCallCount = 0
        coEvery {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(),
                walletInitMode = any(), setup = any(),
                isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        } answers {
            newCallCount++
            for (a in args) {
                if (a is WalletInitMode) capturedModes.add(a)
            }
            if (newCallCount == 1) throw IllegalStateException("Another synchronizer with SynchronizerKey")
            mockSynchronizer
        }

        adapter = createAdapter()

        capturedProcessorErrorHandler?.invoke(
            SQLiteDatabaseCorruptException("database disk image is malformed")
        )

        coVerify(timeout = VERIFY_TIMEOUT, atLeast = 2) {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(),
                walletInitMode = any(), setup = any(),
                isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        }
        assertTrue("Should have at least 2 attempts", capturedModes.size >= 2)
        assertTrue(
            "All attempts must use RestoreWallet",
            capturedModes.all { it == WalletInitMode.RestoreWallet }
        )
        verify(timeout = VERIFY_TIMEOUT, atLeast = 1) { mockSynchronizer.processorInfo }
    }

    @Test
    fun recovery_newCancellation_doesNotRetryOrResubscribe() = runTest(dispatcher) {
        val newCallCount = java.util.concurrent.atomic.AtomicInteger(0)
        val secondNewCall = java.util.concurrent.CountDownLatch(1)
        coEvery {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(),
                walletInitMode = any(), setup = any(),
                isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        } coAnswers {
            if (newCallCount.incrementAndGet() > 1) {
                secondNewCall.countDown()
            }
            throw CancellationException("Synchronizer.new cancelled")
        }

        adapter = createAdapter()

        capturedProcessorErrorHandler?.invoke(
            SQLiteDatabaseCorruptException("database disk image is malformed")
        )

        coVerify(timeout = VERIFY_TIMEOUT) { Synchronizer.erase(any(), any(), any()) }
        coVerify(timeout = VERIFY_TIMEOUT, exactly = 1) {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(),
                walletInitMode = any(), setup = any(),
                isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        }
        assertFalse(
            "Cancellation from Synchronizer.new must not be treated as retryable failure",
            secondNewCall.await(RETRY_DELAY_GUARD_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
        )
        assertEquals(1, newCallCount.get())
        verify(timeout = 500, exactly = 0) { mockSynchronizer.processorInfo }
    }

    @Test
    fun recovery_usesOriginalBirthdayFromRestoreSettings() = runTest(dispatcher) {
        var capturedBirthday: BlockHeight? = null
        coEvery {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(),
                walletInitMode = any(), setup = any(),
                isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        } answers {
            // Find birthday by checking all args for BlockHeight type
            for (i in 0 until args.size) {
                val a = args[i]
                if (a is BlockHeight) {
                    capturedBirthday = a
                    break
                }
            }
            mockSynchronizer
        }

        adapter = createAdapter()

        capturedProcessorErrorHandler?.invoke(
            SQLiteDatabaseCorruptException("database disk image is malformed")
        )

        coVerify(timeout = VERIFY_TIMEOUT) {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(),
                walletInitMode = any(), setup = any(),
                isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        }
        assertEquals(2000000L, capturedBirthday?.value)
        verify(timeout = VERIFY_TIMEOUT, atLeast = 1) { mockSynchronizer.processorInfo }
    }

    // --- Erase failure ---

    @Test
    fun recovery_failedErase_setsNotSyncedState() = runTest(dispatcher) {
        coEvery {
            Synchronizer.erase(any(), any(), any())
        } throws IllegalStateException("synchronizer still active")

        adapter = createAdapter()

        capturedProcessorErrorHandler?.invoke(
            SQLiteDatabaseCorruptException("database disk image is malformed")
        )

        // Wait for all 3 erase retries to complete — this means recovery has finished
        coVerify(timeout = VERIFY_TIMEOUT, exactly = 3) { Synchronizer.erase(any(), any(), any()) }
        // Synchronizer.new should NOT be called after failed erase
        coVerify(timeout = 500, exactly = 0) {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(),
                walletInitMode = any(), setup = any(),
                isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        }
        assertTrue(
            "Should be NotSynced after failed erase",
            adapter.balanceState is AdapterState.NotSynced
        )
    }

    @Test
    fun recovery_eraseCancellation_doesNotRetryOrCreateSynchronizer() = runTest(dispatcher) {
        val eraseCallCount = java.util.concurrent.atomic.AtomicInteger(0)
        val secondEraseCall = java.util.concurrent.CountDownLatch(1)
        coEvery {
            Synchronizer.erase(any(), any(), any())
        } coAnswers {
            if (eraseCallCount.incrementAndGet() > 1) {
                secondEraseCall.countDown()
            }
            throw CancellationException("erase cancelled")
        }

        adapter = createAdapter()

        capturedProcessorErrorHandler?.invoke(
            SQLiteDatabaseCorruptException("database disk image is malformed")
        )

        coVerify(timeout = VERIFY_TIMEOUT, exactly = 1) { Synchronizer.erase(any(), any(), any()) }
        assertFalse(
            "Cancellation from erase must not be treated as retryable IllegalStateException",
            secondEraseCall.await(RETRY_DELAY_GUARD_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
        )
        assertEquals(1, eraseCallCount.get())
        coVerify(exactly = 0) {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(),
                walletInitMode = any(), setup = any(),
                isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        }
    }

    // --- Concurrency guard ---

    @Test
    fun recovery_concurrentCorruptions_onlyOneRecoveryRuns() = runTest(dispatcher) {
        val eraseStarted = java.util.concurrent.CountDownLatch(1)
        val releaseErase = java.util.concurrent.CountDownLatch(1)
        coEvery {
            Synchronizer.erase(any(), any(), any())
        } coAnswers {
            eraseStarted.countDown()
            if (!releaseErase.await(VERIFY_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                throw AssertionError("Timed out waiting to release erase")
            }
            true
        }

        adapter = createAdapter()

        val error = SQLiteDatabaseCorruptException("database disk image is malformed")
        capturedProcessorErrorHandler?.invoke(error)
        assertTrue(
            "Recovery must start before the second corruption is reported",
            eraseStarted.await(VERIFY_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
        )
        capturedCriticalErrorHandler?.invoke(error)

        coVerify(timeout = VERIFY_TIMEOUT, exactly = 1) { Synchronizer.erase(any(), any(), any()) }
        releaseErase.countDown()
        verifyRecoveryResubscribed()
    }

    @Test
    fun recovery_stopAfterNewSynchronizer_doesNotResubscribeClosedSynchronizer() = runTest(dispatcher) {
        val getAccountsStarted = java.util.concurrent.CountDownLatch(1)
        val releaseGetAccounts = java.util.concurrent.CountDownLatch(1)
        val recoverySynchronizer = createMockSynchronizer().also { synchronizer ->
            coEvery { synchronizer.getAccounts() } coAnswers {
                getAccountsStarted.countDown()
                if (!releaseGetAccounts.await(VERIFY_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    throw AssertionError("Timed out waiting to release getAccounts")
                }
                emptyList()
            }
        }
        coEvery {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(),
                walletInitMode = any(), setup = any(),
                isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        } returns recoverySynchronizer

        adapter = createAdapter()

        capturedProcessorErrorHandler?.invoke(
            SQLiteDatabaseCorruptException("database disk image is malformed")
        )
        assertTrue(
            "Recovery synchronizer must be installed before stop()",
            getAccountsStarted.await(VERIFY_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
        )

        adapter.stop()
        releaseGetAccounts.countDown()

        verify(timeout = VERIFY_TIMEOUT, atLeast = 1) { recoverySynchronizer.close() }
        verify(timeout = 500, exactly = 0) { recoverySynchronizer.processorInfo }
    }

    // --- Zombie adapter (MOBILE-587) ---

    @Test
    fun stop_thenEnterForeground_doesNotRestartSynchronizer() = runTest(dispatcher) {
        val bgStateFlow = MutableStateFlow<BackgroundManagerState>(BackgroundManagerState.Unknown)
        every { backgroundManager.stateFlow } returns bgStateFlow

        var zombieRestartCount = 0
        coEvery {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(), walletInitMode = any(),
                setup = any(), isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        } answers {
            zombieRestartCount++
            mockSynchronizer
        }

        adapter = createAdapter()

        // Simulate AdapterManager stopping the old adapter during wallet switch
        adapter.stop()
        statusFlow.value = Synchronizer.Status.STOPPED

        // Simulate app returning to foreground — stopped adapter must NOT react
        // scope is cancelled so the stateFlow collector is dead
        bgStateFlow.value = BackgroundManagerState.EnterForeground

        coVerify(exactly = 0, timeout = VERIFY_TIMEOUT) {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(), walletInitMode = any(),
                setup = any(), isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        }
        assertEquals(
            "Stopped adapter must not restart synchronizer on foreground event",
            0,
            zombieRestartCount
        )
    }

    @Test
    fun stop_whileStartInFlight_cancelsStart() = runTest(dispatcher) {
        val bgStateFlow = MutableStateFlow<BackgroundManagerState>(BackgroundManagerState.Unknown)
        every { backgroundManager.stateFlow } returns bgStateFlow

        val startReached = java.util.concurrent.CountDownLatch(1)
        val cancelled = java.util.concurrent.CountDownLatch(1)
        coEvery {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(), walletInitMode = any(),
                setup = any(), isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        } coAnswers {
            startReached.countDown()
            try {
                kotlinx.coroutines.suspendCancellableCoroutine<Nothing> { }
            } finally {
                cancelled.countDown()
            }
        }

        adapter = createAdapter()

        statusFlow.value = Synchronizer.Status.STOPPED
        bgStateFlow.value = BackgroundManagerState.EnterForeground

        assertTrue(
            "Synchronizer.new() must be reached before stop()",
            startReached.await(VERIFY_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
        )
        adapter.stop()

        assertTrue(
            "Coroutine must be cancelled by stop()",
            cancelled.await(VERIFY_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
        )
    }

    @Test
    fun enterBackground_whileStartInFlight_cancelsStart() = runTest(dispatcher) {
        val bgStateFlow = MutableStateFlow<BackgroundManagerState>(BackgroundManagerState.Unknown)
        every { backgroundManager.stateFlow } returns bgStateFlow

        val startReached = java.util.concurrent.CountDownLatch(1)
        val cancelled = java.util.concurrent.CountDownLatch(1)
        coEvery {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(), walletInitMode = any(),
                setup = any(), isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        } coAnswers {
            startReached.countDown()
            try {
                kotlinx.coroutines.suspendCancellableCoroutine<Nothing> { }
            } finally {
                cancelled.countDown()
            }
        }

        adapter = createAdapter()

        statusFlow.value = Synchronizer.Status.STOPPED
        bgStateFlow.value = BackgroundManagerState.EnterForeground

        assertTrue(
            "Synchronizer.new() must be reached before enterBackground",
            startReached.await(VERIFY_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
        )
        bgStateFlow.value = BackgroundManagerState.EnterBackground

        assertTrue(
            "Coroutine must be cancelled by enterBackground",
            cancelled.await(VERIFY_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
        )
    }

    // --- Pause / resume (background → foreground) ---

    @Test
    fun enterBackground_thenEnterForeground_restartsSynchronizer() = runTest(dispatcher) {
        val bgStateFlow = MutableStateFlow<BackgroundManagerState>(BackgroundManagerState.Unknown)
        every { backgroundManager.stateFlow } returns bgStateFlow

        var restartCount = 0
        coEvery {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(), walletInitMode = any(),
                setup = any(), isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        } answers {
            restartCount++
            mockSynchronizer
        }

        adapter = createAdapter()

        // Simulate app going to background (pause — not full dispose)
        bgStateFlow.value = BackgroundManagerState.EnterBackground
        statusFlow.value = Synchronizer.Status.STOPPED

        // Simulate app returning to foreground
        bgStateFlow.value = BackgroundManagerState.EnterForeground

        coVerify(timeout = VERIFY_TIMEOUT) {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(), walletInitMode = any(),
                setup = any(), isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        }
        assertEquals(
            "Paused adapter must restart synchronizer on foreground event",
            1,
            restartCount
        )
    }

    // --- Sync progress preservation ---

    private fun awaitState(predicate: (AdapterState) -> Boolean) {
        val deadline = System.currentTimeMillis() + VERIFY_TIMEOUT
        while (!predicate(adapter.balanceState)) {
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("Timed out waiting for state, current: ${adapter.balanceState}")
            }
            Thread.sleep(50)
        }
    }

    @Test
    fun onStatus_syncingWhileAlreadySyncingWithProgress_preservesProgress() = runTest(dispatcher) {
        adapter = createAdapter()
        adapter.start()

        // Wait for subscribe() to process initial SYNCING status
        awaitState { it is AdapterState.Syncing }

        // SDK reports progress via onDownloadProgress
        progressFlow.value = PercentDecimal(0.99f)

        // subscriberScope uses Dispatchers.Main (UnconfinedTestDispatcher) — immediate
        awaitState { it is AdapterState.Syncing && it.progress == 99.0 }

        // SDK re-emits SYNCING status (e.g. entering new scan phase)
        statusFlow.value = Synchronizer.Status.SYNCING

        // Progress must NOT be wiped
        val state = adapter.balanceState
        assertTrue(
            "Should still be Syncing after re-emitted SYNCING status",
            state is AdapterState.Syncing
        )
        assertEquals(
            "Progress must be preserved when SDK re-emits SYNCING",
            99.0,
            (state as AdapterState.Syncing).progress
        )
    }

    @Test
    fun onProcessorInfo_syncRangeNearOrchard_preservesSdkProgress() = runTest(dispatcher) {
        adapter = createAdapter()
        adapter.start()

        awaitState { it is AdapterState.Syncing }

        progressFlow.value = PercentDecimal(0.01f)
        awaitState { it is AdapterState.Syncing && it.progress == 1.0 }

        processorInfoFlow.value = CompactBlockProcessor.ProcessorInfo(
            networkBlockHeight = BlockHeight.new(2_881_516L),
            overallSyncRange = BlockHeight.new(1_687_104L)..BlockHeight.new(1_687_104L),
            firstUnenhancedHeight = null
        )

        awaitState { it is AdapterState.Syncing && it.blocksRemained != null }
        val state = adapter.balanceState as AdapterState.Syncing
        assertEquals(
            "ProcessorInfo range must not be treated as sync progress",
            1.0,
            state.progress
        )
    }

    @Test
    fun blocksRemained_scalesInverselyWithSdkProgress() = runTest(dispatcher) {
        adapter = createAdapter()
        adapter.start()

        awaitState { it is AdapterState.Syncing }

        // accountBirthday from mock checkpoint = 2_500_000; networkHeight 3_500_000 → totalBlocks = 1_000_000
        processorInfoFlow.value = CompactBlockProcessor.ProcessorInfo(
            networkBlockHeight = BlockHeight.new(3_500_000L),
            overallSyncRange = null,
            firstUnenhancedHeight = null
        )
        awaitState { it is AdapterState.Syncing && it.blocksRemained == 1_000_000L }

        progressFlow.value = PercentDecimal(0.5f)
        awaitState {
            it is AdapterState.Syncing && it.progress == 50.0 && it.blocksRemained == 500_000L
        }
    }

    @Test
    fun onProcessorInfo_afterSynced_doesNotRestoreStaleBlocksRemaining() = runTest(dispatcher) {
        adapter = createAdapter()
        adapter.start()

        awaitState { it is AdapterState.Syncing }

        processorInfoFlow.value = CompactBlockProcessor.ProcessorInfo(
            networkBlockHeight = BlockHeight.new(3_500_000L),
            overallSyncRange = null,
            firstUnenhancedHeight = null
        )
        awaitState { it is AdapterState.Syncing && it.blocksRemained == 1_000_000L }

        statusFlow.value = Synchronizer.Status.SYNCED
        awaitState { it is AdapterState.Synced }

        processorInfoFlow.value = CompactBlockProcessor.ProcessorInfo(
            networkBlockHeight = BlockHeight.new(3_500_001L),
            overallSyncRange = null,
            firstUnenhancedHeight = null
        )
        advanceUntilIdle()

        assertEquals(AdapterState.Synced, adapter.balanceState)
    }

    @Test
    fun onStatus_syncingAfterSynced_allowsFreshProgressUpdates() = runTest(dispatcher) {
        adapter = createAdapter()
        adapter.start()

        awaitState { it is AdapterState.Syncing }

        statusFlow.value = Synchronizer.Status.SYNCED
        awaitState { it is AdapterState.Synced }

        statusFlow.value = Synchronizer.Status.SYNCING
        awaitState { it is AdapterState.Syncing }

        processorInfoFlow.value = CompactBlockProcessor.ProcessorInfo(
            networkBlockHeight = BlockHeight.new(3_500_000L),
            overallSyncRange = null,
            firstUnenhancedHeight = null
        )
        progressFlow.value = PercentDecimal(0.5f)

        awaitState {
            it is AdapterState.Syncing && it.progress == 50.0 && it.blocksRemained == 500_000L
        }
    }

    @Test
    fun onStatus_syncingFromNonSyncingState_createsFreshSyncing() = runTest(dispatcher) {
        adapter = createAdapter()
        adapter.start()

        // Wait for subscribe() to process initial SYNCING status
        awaitState { it is AdapterState.Syncing }

        // Move to a non-syncing state without triggering synchronizer recreation.
        statusFlow.value = Synchronizer.Status.DISCONNECTED
        awaitState { it is AdapterState.NotSynced }

        // Transition to SYNCING — should create fresh Syncing (no progress)
        statusFlow.value = Synchronizer.Status.SYNCING
        awaitState { it is AdapterState.Syncing }

        val state = adapter.balanceState as AdapterState.Syncing
        assertEquals("Fresh Syncing should have no progress", null, state.progress)
    }

    // --- Erase retry ---

    @Test
    fun eraseRetry_succeedsOnSecondAttempt() = runTest(dispatcher) {
        var eraseCallCount = 0
        coEvery {
            Synchronizer.erase(any(), any(), any())
        } answers {
            eraseCallCount++
            if (eraseCallCount == 1) throw IllegalStateException("synchronizer still active")
            true
        }

        adapter = createAdapter()

        capturedProcessorErrorHandler?.invoke(
            SQLiteDatabaseCorruptException("database disk image is malformed")
        )

        coVerify(timeout = VERIFY_TIMEOUT) {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(), walletInitMode = any(),
                setup = any(), isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        }
        assertEquals(2, eraseCallCount)
        verify(timeout = VERIFY_TIMEOUT, atLeast = 1) { mockSynchronizer.processorInfo }
    }

    private fun verifySynchronizerNew() {
        coVerify(timeout = VERIFY_TIMEOUT) {
            Synchronizer.new(
                context = any(), zcashNetwork = any(), alias = any(),
                lightWalletEndpoint = any(), birthday = any(), walletInitMode = any(),
                setup = any(), isTorEnabled = any(), isExchangeRateEnabled = any()
            )
        }
    }

    private fun verifyRecoveryResubscribed() {
        verifySynchronizerNew()
        verify(timeout = VERIFY_TIMEOUT, atLeast = 1) { mockSynchronizer.processorInfo }
    }

    private fun createMockSynchronizer(): SdkSynchronizer {
        return mockk<SdkSynchronizer>(relaxed = true) {
            every { status } returns statusFlow
            every { progress } returns progressFlow
            every { walletBalances } returns walletBalancesFlow
            every { processorInfo } returns processorInfoFlow
            every { allTransactions } returns allTransactionsFlow
            every { coroutineScope } returns testScope
            every { latestHeight } returns null
        }
    }
}
