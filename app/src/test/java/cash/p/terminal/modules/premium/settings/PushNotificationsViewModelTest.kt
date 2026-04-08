package cash.p.terminal.modules.premium.settings

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.SolanaRpcSourceManager
import cash.p.terminal.wallet.MarketKitWrapper
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PushNotificationsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)
    private val testDispatcherProvider = TestDispatcherProvider(dispatcher, testScope)

    private val btcBlockchainManager = mockk<BtcBlockchainManager>(relaxed = true)
    private val evmBlockchainManager = mockk<EvmBlockchainManager>(relaxed = true)
    private val solanaRpcSourceManager = mockk<SolanaRpcSourceManager>(relaxed = true)
    private val marketKit = mockk<MarketKitWrapper>(relaxed = true)
    private val localStorage = mockk<ILocalStorage>(relaxed = true)

    @Before
    fun setUp() {
        clearMocks(
            btcBlockchainManager,
            evmBlockchainManager,
            solanaRpcSourceManager,
            marketKit,
            localStorage,
        )

        every { localStorage.pushNotificationsEnabled } returns false
        every { localStorage.pushPollingInterval } returns PollingInterval.MIN_5
        every { localStorage.pushShowBlockchainName } returns true
        every { localStorage.pushShowCoinAmount } returns true
        every { localStorage.pushShowFiatAmount } returns true
        every { localStorage.pushEnabledBlockchainUids } returns emptySet()

        every { btcBlockchainManager.allBlockchains } returns emptyList()
        every { evmBlockchainManager.allBlockchains } returns emptyList()
        every { solanaRpcSourceManager.blockchain } returns null
        every { marketKit.blockchains(any()) } returns emptyList()

        stopKoin()
        startKoin {
            modules(
                module {
                    single<DispatcherProvider> { testDispatcherProvider }
                }
            )
        }

        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PushNotificationsViewModel(
        btcBlockchainManager = btcBlockchainManager,
        evmBlockchainManager = evmBlockchainManager,
        solanaRpcSourceManager = solanaRpcSourceManager,
        marketKit = marketKit,
        dispatcherProvider = testDispatcherProvider,
        localStorage = localStorage,
    )

    @Test
    fun setShowNotifications_enabled_persistsToStorage() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setShowNotifications(true)
        advanceUntilIdle()

        verify { localStorage.pushNotificationsEnabled = true }
        assertTrue(viewModel.uiState.showNotifications)
    }

    @Test
    fun setPollingInterval_min5_persistsToStorage() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setPollingInterval(PollingInterval.MIN_5)
        advanceUntilIdle()

        verify { localStorage.pushPollingInterval = PollingInterval.MIN_5 }
        assertEquals(PollingInterval.MIN_5, viewModel.uiState.pollingInterval)
    }

    @Test
    fun setBlockchainNotifications_disable_removesFromEnabledSet() = runTest(dispatcher) {
        every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin", "ethereum")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setBlockchainNotifications("bitcoin", false)
        advanceUntilIdle()

        verify { localStorage.pushEnabledBlockchainUids = setOf("ethereum") }
    }

    @Test
    fun setBlockchainNotifications_enable_addsToEnabledSet() = runTest(dispatcher) {
        every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setBlockchainNotifications("ethereum", true)
        advanceUntilIdle()

        verify { localStorage.pushEnabledBlockchainUids = setOf("bitcoin", "ethereum") }
    }

    @Test
    fun setShowBlockchainName_false_persistsToStorage() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setShowBlockchainName(false)
        advanceUntilIdle()

        verify { localStorage.pushShowBlockchainName = false }
        assertFalse(viewModel.uiState.showBlockchainName)
    }

    @Test
    fun loadBlockchains_firstOpen_enablesAllBlockchains() = runTest(dispatcher) {
        every { localStorage.pushBlockchainsConfigured } returns false
        every { btcBlockchainManager.allBlockchains } returns listOf(
            blockchain(BlockchainType.Bitcoin, "Bitcoin")
        )
        every { evmBlockchainManager.allBlockchains } returns listOf(
            blockchain(BlockchainType.Ethereum, "Ethereum")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        verify { localStorage.pushEnabledBlockchainUids = match { "bitcoin" in it && "ethereum" in it } }
        verify { localStorage.pushBlockchainsConfigured = true }
        assertTrue(viewModel.uiState.blockchains.all { it.notificationsEnabled })
    }

    @Test
    fun loadBlockchains_userClearedAllBlockchains_doesNotAutoEnableAgain() = runTest(dispatcher) {
        every { localStorage.pushBlockchainsConfigured } returns true
        every { localStorage.pushEnabledBlockchainUids } returns emptySet()
        every { btcBlockchainManager.allBlockchains } returns listOf(
            blockchain(BlockchainType.Bitcoin, "Bitcoin")
        )
        every { evmBlockchainManager.allBlockchains } returns listOf(
            blockchain(BlockchainType.Ethereum, "Ethereum")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        // pushBlockchainsConfigured was already true, so no auto-enable should happen.
        // The only setter call for pushEnabledBlockchainUids should NOT occur during loadBlockchains.
        verify(exactly = 0) { localStorage.pushEnabledBlockchainUids = any() }
        assertTrue(viewModel.uiState.blockchains.all { !it.notificationsEnabled })
    }

    private fun blockchain(type: BlockchainType, name: String) = Blockchain(type, name, null)
}
