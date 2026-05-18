package cash.p.terminal.core.managers

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class RateAppManagerTest {

    private lateinit var walletManager: IWalletManager
    private lateinit var adapterManager: IAdapterManager
    private lateinit var localStorage: ILocalStorage

    @Before
    fun setUp() {
        walletManager = mockk(relaxed = true)
        adapterManager = mockk(relaxed = true)
        localStorage = mockk(relaxed = true)

        every { localStorage.isCalculatorModeEnabled } returns false
        every { localStorage.appLaunchCount } returns RateAppManager.MIN_LAUNCH_COUNT
        every { localStorage.rateAppLastRequestTime } returns 0
        every { localStorage.appLaunchCount = any() } just Runs
        every { localStorage.rateAppLastRequestTime = any() } just Runs
    }

    @Test
    fun onAppLaunch_calculatorModeEnabled_doesNotIncrementLaunchCount() {
        enableCalculatorMode()

        createManager().onAppLaunch()

        verify(exactly = 0) { localStorage.appLaunchCount = any() }
    }

    @Test
    fun onCountdownPass_calculatorModeEnabled_doesNotInspectWalletsOrShowRateRequest() = runTest {
        enableCalculatorMode()
        val manager = createManager()
        manager.onBalancePageActive()

        manager.onCountdownPass()

        verify(exactly = 0) { walletManager.activeWallets }
        verify(exactly = 0) { localStorage.rateAppLastRequestTime = any() }
    }

    @Test
    fun onBalancePageActive_calculatorModeEnabledWithAllowedRequest_doesNotShowRateRequest() = runTest {
        stubEligibleWallets()
        val manager = createManager()
        manager.onCountdownPass()
        enableCalculatorMode()

        manager.onBalancePageActive()

        verify(exactly = 0) { localStorage.rateAppLastRequestTime = any() }
    }

    @Test
    fun onCountdownPass_balancePageActiveAndEligible_showsRateRequest() = runTest {
        stubEligibleWallets()
        val manager = createManager()
        manager.onBalancePageActive()
        val request = async {
            manager.showRateAppFlow.first { it }
        }

        manager.onCountdownPass()

        assertEquals(true, request.await())
        verify { localStorage.rateAppLastRequestTime = any() }
    }

    private fun createManager() = RateAppManager(
        walletManager = walletManager,
        adapterManager = adapterManager,
        localStorage = localStorage,
    )

    private fun enableCalculatorMode() {
        every { localStorage.isCalculatorModeEnabled } returns true
    }

    private fun stubEligibleWallets() {
        val walletOne = mockk<Wallet>()
        val walletTwo = mockk<Wallet>()
        val adapter = mockk<IBalanceAdapter> {
            every { balanceData } returns BalanceData(available = BigDecimal.ONE)
        }
        every { walletManager.activeWallets } returns listOf(walletOne, walletTwo)
        every { adapterManager.getBalanceAdapterForWallet(walletOne) } returns adapter
        every { adapterManager.getBalanceAdapterForWallet(walletTwo) } returns null
    }
}
