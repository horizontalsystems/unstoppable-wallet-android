package cash.p.terminal.core.notifications

import android.app.Application
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.BackgroundKeepAliveManager
import cash.p.terminal.modules.premium.settings.PollingInterval
import cash.p.terminal.premium.domain.usecase.CheckPremiumUseCase
import cash.p.terminal.premium.domain.usecase.PremiumType
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionNotificationCoordinatorTest {

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val localStorage = mockk<ILocalStorage>(relaxed = true)
    private val notificationManager = mockk<TransactionNotificationManager>(relaxed = true)
    private val backgroundManager = mockk<BackgroundManager>(relaxed = true)
    private val checkPremiumUseCase = mockk<CheckPremiumUseCase>(relaxed = true)
    private val keepAliveManager = mockk<BackgroundKeepAliveManager>(relaxed = true)
    private val walletManager = mockk<IWalletManager>(relaxed = true)
    private val backgroundStateFlow = MutableStateFlow<BackgroundManagerState>(BackgroundManagerState.Unknown)

    private var capturedCallback: (() -> Unit)? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { backgroundManager.stateFlow } returns backgroundStateFlow
        every { backgroundManager.onBeforeEnterBackground = any() } answers {
            capturedCallback = firstArg()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        capturedCallback = null
    }

    private fun createCoordinator(): TransactionNotificationCoordinator {
        val coordinator = TransactionNotificationCoordinator(
            application = application,
            localStorage = localStorage,
            notificationManager = notificationManager,
            backgroundManager = backgroundManager,
            checkPremiumUseCase = checkPremiumUseCase,
            keepAliveManager = keepAliveManager,
            walletManager = walletManager,
        )
        coordinator.start()
        return coordinator
    }

    private fun simulateEnterBackground() {
        assertNotNull(capturedCallback, "onBeforeEnterBackground not set")
        capturedCallback?.invoke()
    }

    private fun setupAllConditionsMet() {
        every { checkPremiumUseCase.getPremiumType() } returns PremiumType.PIRATE
        every { localStorage.pushNotificationsEnabled } returns true
        every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin")
        every { notificationManager.hasNotificationPermission() } returns true
        every { notificationManager.isTransactionChannelEnabled() } returns true
        every { notificationManager.isServiceChannelEnabled() } returns true
    }

    @Test
    fun onEnterBackground_allConditionsMet_startsService() {
        setupAllConditionsMet()

        createCoordinator()
        simulateEnterBackground()

        verify { application.startForegroundService(any()) }
    }

    @Test
    fun onEnterBackground_noPremium_doesNotStartService() {
        every { checkPremiumUseCase.getPremiumType() } returns PremiumType.NONE
        every { localStorage.pushNotificationsEnabled } returns true
        every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin")
        every { notificationManager.hasNotificationPermission() } returns true

        createCoordinator()
        simulateEnterBackground()

        verify(exactly = 0) { application.startForegroundService(any()) }
    }

    @Test
    fun onEnterBackground_notificationsDisabled_doesNotStartService() {
        every { checkPremiumUseCase.getPremiumType() } returns PremiumType.PIRATE
        every { localStorage.pushNotificationsEnabled } returns false
        every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin")
        every { notificationManager.hasNotificationPermission() } returns true

        createCoordinator()
        simulateEnterBackground()

        verify(exactly = 0) { application.startForegroundService(any()) }
    }

    @Test
    fun onEnterBackground_noEnabledBlockchains_doesNotStartService() {
        every { checkPremiumUseCase.getPremiumType() } returns PremiumType.PIRATE
        every { localStorage.pushNotificationsEnabled } returns true
        every { localStorage.pushEnabledBlockchainUids } returns emptySet()
        every { notificationManager.hasNotificationPermission() } returns true

        createCoordinator()
        simulateEnterBackground()

        verify(exactly = 0) { application.startForegroundService(any()) }
    }

    @Test
    fun onEnterBackground_noNotificationPermission_doesNotStartService() {
        every { localStorage.pushNotificationsEnabled } returns true
        every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin")
        every { notificationManager.hasNotificationPermission() } returns false

        createCoordinator()
        simulateEnterBackground()

        verify(exactly = 0) { application.startForegroundService(any()) }
    }

    @Test
    fun startService_realtimeMode_setsKeepAliveBeforeServiceStart() {
        setupAllConditionsMet()
        every { localStorage.pushPollingInterval } returns PollingInterval.REALTIME

        val btcWallet = mockk<Wallet>(relaxed = true)
        every { btcWallet.token.blockchainType } returns BlockchainType.Bitcoin
        every { walletManager.activeWallets } returns listOf(btcWallet)

        createCoordinator()
        simulateEnterBackground()

        io.mockk.verifyOrder {
            keepAliveManager.setKeepAlive(setOf(BlockchainType.Bitcoin))
            application.startForegroundService(any())
        }
    }

    @Test
    fun startService_pollingMode_doesNotSetKeepAliveForNonSolana() {
        setupAllConditionsMet()
        every { localStorage.pushPollingInterval } returns PollingInterval.MIN_5

        val btcWallet = mockk<Wallet>(relaxed = true)
        every { btcWallet.token.blockchainType } returns BlockchainType.Bitcoin
        every { walletManager.activeWallets } returns listOf(btcWallet)

        createCoordinator()
        simulateEnterBackground()

        verify { keepAliveManager.setKeepAlive(emptySet()) }
        verify { application.startForegroundService(any()) }
    }

    @Test
    fun startService_pollingMode_solanaDoesNotGetKeepAlive() {
        setupAllConditionsMet()
        every { localStorage.pushPollingInterval } returns PollingInterval.MIN_5
        every { localStorage.pushEnabledBlockchainUids } returns setOf("solana")

        val solWallet = mockk<Wallet>(relaxed = true)
        every { solWallet.token.blockchainType } returns BlockchainType.Solana
        every { walletManager.activeWallets } returns listOf(solWallet)

        createCoordinator()
        simulateEnterBackground()

        verify { keepAliveManager.setKeepAlive(emptySet()) }
        verify { application.startForegroundService(any()) }
    }

    @Test
    fun startService_pollingMode_mixedChains_noKeepAliveForAnyone() {
        setupAllConditionsMet()
        every { localStorage.pushPollingInterval } returns PollingInterval.MIN_5
        every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin", "solana")

        val btcWallet = mockk<Wallet>(relaxed = true)
        every { btcWallet.token.blockchainType } returns BlockchainType.Bitcoin
        val solWallet = mockk<Wallet>(relaxed = true)
        every { solWallet.token.blockchainType } returns BlockchainType.Solana
        every { walletManager.activeWallets } returns listOf(btcWallet, solWallet)

        createCoordinator()
        simulateEnterBackground()

        verify { keepAliveManager.setKeepAlive(emptySet()) }
    }

    @Test
    fun startService_failedOnEnterBackground_enterForeground_doesNotStopService() {
        setupAllConditionsMet()
        every { application.startForegroundService(any()) } throws RuntimeException("start failed")

        createCoordinator()
        simulateEnterBackground()

        backgroundStateFlow.value = BackgroundManagerState.EnterForeground
        dispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { application.startService(any()) }
    }
}
