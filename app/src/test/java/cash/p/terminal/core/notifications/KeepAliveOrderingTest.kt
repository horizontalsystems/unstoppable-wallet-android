package cash.p.terminal.core.notifications

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies the ordering contract: keep-alive is set before kits
 * receive EnterBackground.
 *
 * Simulates the BackgroundManager's sequential execution:
 * onBeforeEnterBackground → stateFlow.emit(EnterBackground)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KeepAliveOrderingTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun enterBackground_keepAliveSetBeforeKitsReceiveEvent() = runTest(dispatcher) {
        val keepAliveManager = BackgroundKeepAliveManager()
        val stateFlow = MutableStateFlow<BackgroundManagerState>(BackgroundManagerState.Unknown)

        val localStorage = mockk<ILocalStorage>(relaxed = true)
        every { localStorage.pushNotificationsEnabled } returns true
        every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin")
        every { localStorage.pushPollingInterval } returns PollingInterval.REALTIME

        val notificationManager = mockk<TransactionNotificationManager>(relaxed = true)
        every { notificationManager.hasNotificationPermission() } returns true
        every { notificationManager.isTransactionChannelEnabled() } returns true
        every { notificationManager.isServiceChannelEnabled() } returns true

        val checkPremiumUseCase = mockk<CheckPremiumUseCase>(relaxed = true)
        every { checkPremiumUseCase.getPremiumType() } returns PremiumType.PIRATE

        val btcWallet = mockk<Wallet>(relaxed = true)
        every { btcWallet.token.blockchainType } returns BlockchainType.Bitcoin
        val walletManager = mockk<IWalletManager>(relaxed = true)
        every { walletManager.activeWallets } returns listOf(btcWallet)

        val backgroundManager = mockk<BackgroundManager>(relaxed = true)
        every { backgroundManager.stateFlow } returns stateFlow

        var capturedCallback: (() -> Unit)? = null
        every { backgroundManager.onBeforeEnterBackground = any() } answers {
            capturedCallback = firstArg()
        }

        TransactionNotificationCoordinator(
            application = mockk(relaxed = true),
            localStorage = localStorage,
            notificationManager = notificationManager,
            backgroundManager = backgroundManager,
            checkPremiumUseCase = checkPremiumUseCase,
            keepAliveManager = keepAliveManager,
            walletManager = walletManager,
        ).start()

        var keepAliveWasSetWhenKitReceivedEvent = false

        backgroundScope.launch {
            stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterBackground) {
                    keepAliveWasSetWhenKitReceivedEvent =
                        keepAliveManager.isKeepAlive(BlockchainType.Bitcoin)
                }
            }
        }
        advanceUntilIdle()

        // Simulate BackgroundManager.onActivityStopped: callback then emit, sequentially
        capturedCallback?.invoke()
        stateFlow.value = BackgroundManagerState.EnterBackground
        advanceUntilIdle()

        assertTrue(
            keepAliveWasSetWhenKitReceivedEvent,
            "Keep-alive must be set before kits receive EnterBackground"
        )
    }

    @Test
    fun enterBackground_pollingMode_solanaKeepAliveNOTSet() = runTest(dispatcher) {
        val keepAliveManager = BackgroundKeepAliveManager()
        val stateFlow = MutableStateFlow<BackgroundManagerState>(BackgroundManagerState.Unknown)

        val localStorage = mockk<ILocalStorage>(relaxed = true)
        every { localStorage.pushNotificationsEnabled } returns true
        every { localStorage.pushEnabledBlockchainUids } returns setOf("solana")
        every { localStorage.pushPollingInterval } returns PollingInterval.MIN_5

        val notificationManager = mockk<TransactionNotificationManager>(relaxed = true)
        every { notificationManager.hasNotificationPermission() } returns true
        every { notificationManager.isTransactionChannelEnabled() } returns true
        every { notificationManager.isServiceChannelEnabled() } returns true

        val checkPremiumUseCase = mockk<CheckPremiumUseCase>(relaxed = true)
        every { checkPremiumUseCase.getPremiumType() } returns PremiumType.PIRATE

        val solWallet = mockk<Wallet>(relaxed = true)
        every { solWallet.token.blockchainType } returns BlockchainType.Solana
        val walletManager = mockk<IWalletManager>(relaxed = true)
        every { walletManager.activeWallets } returns listOf(solWallet)

        val backgroundManager = mockk<BackgroundManager>(relaxed = true)
        every { backgroundManager.stateFlow } returns stateFlow

        var capturedCallback: (() -> Unit)? = null
        every { backgroundManager.onBeforeEnterBackground = any() } answers {
            capturedCallback = firstArg()
        }

        TransactionNotificationCoordinator(
            application = mockk(relaxed = true),
            localStorage = localStorage,
            notificationManager = notificationManager,
            backgroundManager = backgroundManager,
            checkPremiumUseCase = checkPremiumUseCase,
            keepAliveManager = keepAliveManager,
            walletManager = walletManager,
        ).start()

        var solanaKeepAliveWasSet = false

        backgroundScope.launch {
            stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterBackground) {
                    solanaKeepAliveWasSet =
                        keepAliveManager.isKeepAlive(BlockchainType.Solana)
                }
            }
        }
        advanceUntilIdle()

        capturedCallback?.invoke()
        stateFlow.value = BackgroundManagerState.EnterBackground
        advanceUntilIdle()

        assertFalse(
            solanaKeepAliveWasSet,
            "Solana keep-alive must NOT be set in polling mode"
        )
    }
}
