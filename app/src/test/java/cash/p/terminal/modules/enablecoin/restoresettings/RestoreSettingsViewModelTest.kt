package cash.p.terminal.modules.enablecoin.restoresettings

import cash.p.terminal.core.managers.AccountCleaner
import cash.p.terminal.wallet.Clearable
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class RestoreSettingsViewModelTest : KoinTest {

    private val dispatcher = StandardTestDispatcher()
    private val service = mockk<RestoreSettingsService>(relaxed = true)
    private val accountCleaner = mockk<AccountCleaner>(relaxed = true)
    private lateinit var requestSubject: PublishSubject<RestoreSettingsService.Request>

    @get:Rule
    val koinRule = KoinTestRule.create {
        modules(
            module {
                single { accountCleaner }
            }
        )
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(dispatcher)

        requestSubject = PublishSubject.create()
        every { service.requestObservable } returns requestSubject
        every { service.enter(any(), any()) } returns false
        justRun { service.cancel(any()) }
        coEvery { accountCleaner.clearWalletForCurrentAccount(any()) } returns Unit
        coEvery { accountCleaner.clearWalletForAccount(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun handleRequest_birthdayHeightRequest_exposesToken() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle() // wait for init

        val token = token(BlockchainType.Zcash)
        val initialConfig = TokenConfig("123", false)

        requestSubject.onNext(
            RestoreSettingsService.Request(
                token = token,
                requestType = RestoreSettingsService.RequestType.BirthdayHeight,
                initialConfig = initialConfig,
                accountId = "account-id"
            )
        )
        advanceUntilIdle()

        assertEquals(token, viewModel.openTokenConfigure)
        assertEquals(initialConfig, viewModel.consumeInitialConfig())
        assertNull(viewModel.consumeInitialConfig())
    }

    @Test
    fun tokenConfigureOpened_openTokenConfigure_clearsState() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle() // wait for init
        val token = token(BlockchainType.Zcash)

        requestSubject.onNext(
            RestoreSettingsService.Request(
                token = token,
                requestType = RestoreSettingsService.RequestType.BirthdayHeight,
                initialConfig = null
            )
        )
        advanceUntilIdle()

        viewModel.tokenConfigureOpened()

        assertNull(viewModel.openTokenConfigure)
    }

    @Test
    fun onCancelEnterBirthdayHeight_activeRequest_delegatesToService() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle() // wait for init
        val token = token(BlockchainType.Zcash)

        requestSubject.onNext(
            RestoreSettingsService.Request(
                token = token,
                requestType = RestoreSettingsService.RequestType.BirthdayHeight,
                initialConfig = null
            )
        )
        advanceUntilIdle()

        viewModel.onCancelEnterBirthdayHeight()

        verify(exactly = 1) { service.cancel(token) }
    }

    @Test
    fun onEnter_heightChanged_clearsWalletBeforeService() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle() // wait for init

        val token = token(BlockchainType.Zcash)
        val initialConfig = TokenConfig("100", false)

        requestSubject.onNext(
            RestoreSettingsService.Request(
                token = token,
                requestType = RestoreSettingsService.RequestType.BirthdayHeight,
                initialConfig = initialConfig,
                accountId = "account-id"
            )
        )
        advanceUntilIdle()

        val newConfig = TokenConfig("200", false)
        viewModel.onEnter(newConfig)
        advanceUntilIdle()

        coVerifyOrder {
            accountCleaner.clearWalletForAccount("account-id", token)
            service.enter(newConfig, token)
        }
    }

    @Test
    fun onEnter_secondRequestWhileFirstDialogOpen_usesFirstRequestAndQueuesSecond() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle() // wait for init

            val firstToken = token(BlockchainType.Zcash)
            val secondToken = token(BlockchainType.Litecoin, TokenType.Mweb)
            val firstInitialConfig = TokenConfig("100", false)
            val firstResultConfig = TokenConfig("200", false)
            val secondInitialConfig = TokenConfig("2257920", false)

            requestSubject.onNext(
                RestoreSettingsService.Request(
                    token = firstToken,
                    requestType = RestoreSettingsService.RequestType.BirthdayHeight,
                    initialConfig = firstInitialConfig
                )
            )
            advanceUntilIdle()
            viewModel.tokenConfigureOpened()

            requestSubject.onNext(
                RestoreSettingsService.Request(
                    token = secondToken,
                    requestType = RestoreSettingsService.RequestType.BirthdayHeight,
                    initialConfig = secondInitialConfig
                )
            )
            advanceUntilIdle()

            viewModel.onEnter(firstResultConfig)
            advanceUntilIdle()

            verify(exactly = 1) { service.enter(firstResultConfig, firstToken) }
            assertEquals(secondToken, viewModel.openTokenConfigure)
            assertEquals(secondInitialConfig, viewModel.consumeInitialConfig())
        }

    @Test
    fun onEnter_threeRequestsWhileFirstDialogOpen_processesQueuedRequestsInOrder() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val firstToken = token(BlockchainType.Zcash)
            val secondToken = token(BlockchainType.Litecoin, TokenType.Mweb)
            val thirdToken = token(BlockchainType.Bitcoin)
            val firstResultConfig = TokenConfig("200", false)
            val secondResultConfig = TokenConfig("2257920", false)
            val thirdInitialConfig = TokenConfig("300", false)

            requestSubject.onNext(
                RestoreSettingsService.Request(
                    token = firstToken,
                    requestType = RestoreSettingsService.RequestType.BirthdayHeight,
                    initialConfig = TokenConfig("100", false)
                )
            )
            advanceUntilIdle()
            viewModel.tokenConfigureOpened()

            requestSubject.onNext(
                RestoreSettingsService.Request(
                    token = secondToken,
                    requestType = RestoreSettingsService.RequestType.BirthdayHeight,
                    initialConfig = TokenConfig("2250000", false)
                )
            )
            requestSubject.onNext(
                RestoreSettingsService.Request(
                    token = thirdToken,
                    requestType = RestoreSettingsService.RequestType.BirthdayHeight,
                    initialConfig = thirdInitialConfig
                )
            )
            advanceUntilIdle()

            viewModel.onEnter(firstResultConfig)
            advanceUntilIdle()

            assertEquals(secondToken, viewModel.openTokenConfigure)
            viewModel.tokenConfigureOpened()

            viewModel.onEnter(secondResultConfig)
            advanceUntilIdle()

            assertEquals(thirdToken, viewModel.openTokenConfigure)
            assertEquals(thirdInitialConfig, viewModel.consumeInitialConfig())
        }

    @Test
    fun onEnter_heightUnchanged_skipsClearing() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle() // wait for init

        val token = token(BlockchainType.Zcash)
        val initialConfig = TokenConfig("100", false)

        requestSubject.onNext(
            RestoreSettingsService.Request(
                token = token,
                requestType = RestoreSettingsService.RequestType.BirthdayHeight,
                initialConfig = initialConfig
            )
        )
        advanceUntilIdle()

        viewModel.onEnter(initialConfig)
        advanceUntilIdle()

        verify(exactly = 1) { service.enter(initialConfig, token) }
        coVerify(exactly = 0) { accountCleaner.clearWalletForAccount(any(), any()) }
    }

    @Test
    fun onEnter_heightChanged_returnsBeforeWalletClearCompletes() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val token = token(BlockchainType.Zcash)
        val initialConfig = TokenConfig("100", false)
        val cleanupStarted = CompletableDeferred<Unit>()
        val cleanupRelease = CompletableDeferred<Unit>()
        coEvery { accountCleaner.clearWalletForAccount("account-id", token) } coAnswers {
            cleanupStarted.complete(Unit)
            cleanupRelease.await()
        }
        requestSubject.onNext(
            RestoreSettingsService.Request(
                token = token,
                requestType = RestoreSettingsService.RequestType.BirthdayHeight,
                initialConfig = initialConfig,
                accountId = "account-id"
            )
        )
        advanceUntilIdle()

        withTimeout(100) {
            viewModel.onEnter(TokenConfig("200", false))
        }
        advanceUntilIdle()

        assertEquals(Unit, cleanupStarted.await())
        verify(exactly = 0) { service.enter(any(), any()) }

        cleanupRelease.complete(Unit)
        advanceUntilIdle()

        verify(exactly = 1) { service.enter(TokenConfig("200", false), token) }
    }

    @Test
    fun onEnter_initialConfigMissing_skipsClearingWallet() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle() // wait for init

        val token = token(BlockchainType.Litecoin, TokenType.Mweb)
        val config = TokenConfig("2257920", false)

        requestSubject.onNext(
            RestoreSettingsService.Request(
                token = token,
                requestType = RestoreSettingsService.RequestType.BirthdayHeight,
                initialConfig = null
            )
        )
        advanceUntilIdle()

        viewModel.onEnter(config)
        advanceUntilIdle()

        verify(exactly = 1) { service.enter(config, token) }
        coVerify(exactly = 0) { accountCleaner.clearWalletForAccount(any(), any()) }
    }

    @Test
    fun onEnter_withoutRequest_doesNothing() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle() // wait for init

        viewModel.onEnter(TokenConfig("123", false))
        advanceUntilIdle()

        verify(exactly = 0) { service.enter(any(), any()) }
        coVerify(exactly = 0) { accountCleaner.clearWalletForAccount(any(), any()) }
        verify(exactly = 0) { service.cancel(any()) }
    }

    @Test
    fun onCleared_anyClearables_clearsEveryClearable() {
        val clearable1 = mockk<Clearable>(relaxed = true)
        val clearable2 = mockk<Clearable>(relaxed = true)

        val viewModel = createViewModel(listOf(clearable1, clearable2))

        viewModel.invokeOnClearedForTest()

        verify(exactly = 1) { clearable1.clear() }
        verify(exactly = 1) { clearable2.clear() }
    }

    private fun createViewModel(clearables: List<Clearable> = emptyList()): RestoreSettingsViewModel =
        RestoreSettingsViewModel(service, clearables)

    private fun token(
        blockchainType: BlockchainType,
        tokenType: TokenType = TokenType.Native,
    ): Token {
        return Token(
            coin = Coin(
                uid = "uid-${blockchainType.uid}",
                name = "Coin-${blockchainType.uid}",
                code = blockchainType.uid.uppercase()
            ),
            blockchain = Blockchain(
                type = blockchainType,
                name = blockchainType.uid,
                eip3091url = null
            ),
            type = tokenType,
            decimals = 8
        )
    }

    private fun RestoreSettingsViewModel.invokeOnClearedForTest() {
        RestoreSettingsViewModel::class.java.getDeclaredMethod("onCleared").apply {
            isAccessible = true
            invoke(this@invokeOnClearedForTest)
        }
    }
}
