package cash.p.terminal.modules.pin

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.feature.logging.domain.usecase.DeleteLoggingOnDuressUseCase
import cash.p.terminal.feature.logging.domain.usecase.LogLoginAttemptUseCase
import cash.p.terminal.modules.pin.core.ILockoutManager
import cash.p.terminal.modules.pin.core.LockoutState
import cash.p.terminal.modules.pin.core.OneTimeTimer
import cash.p.terminal.modules.pin.unlock.PinUnlockModule
import cash.p.terminal.modules.pin.unlock.PinUnlockViewModel
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PinUnlockViewModelTest {

    private val pinComponent: IPinComponent = mockk(relaxed = true)
    private val lockoutManager: ILockoutManager = mockk(relaxed = true)
    private val systemInfoManager: ISystemInfoManager = mockk(relaxed = true)
    private val timer: OneTimeTimer = mockk(relaxed = true)
    private val localStorage: ILocalStorage = mockk(relaxed = true)
    private val logLoginAttemptUseCase: LogLoginAttemptUseCase = mockk(relaxed = true)
    private val deleteLoggingOnDuressUseCase: DeleteLoggingOnDuressUseCase = mockk(relaxed = true)
    private val sendZecOnDuressUseCase: SendZecOnDuressUseCase = mockk(relaxed = true)

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { systemInfoManager.biometricAuthSupported } returns false
        every { pinComponent.isBiometricAuthEnabled } returns false
        every { localStorage.pinRandomized } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(initialState: LockoutState = LockoutState.Unlocked(null)): PinUnlockViewModel {
        every { lockoutManager.currentState } returns initialState
        return PinUnlockViewModel(
            pinComponent,
            lockoutManager,
            systemInfoManager,
            timer,
            localStorage,
            logLoginAttemptUseCase,
            deleteLoggingOnDuressUseCase,
            sendZecOnDuressUseCase
        )
    }

    @Test
    fun unlocked_afterFailedThenSuccessfulPin_resetsAttemptsLeft() = runTest(dispatcher) {
        // Start with 4 attempts left (1 failed attempt out of 5)
        val viewModel = createViewModel(LockoutState.Unlocked(attemptsLeft = 4))

        // Simulate successful PIN entry
        every { pinComponent.getPinLevel("123456") } returns 0
        coEvery { pinComponent.unlock("123456", 0) } returns true
        coEvery { logLoginAttemptUseCase.captureLoginPhoto(0) } returns null
        coEvery { logLoginAttemptUseCase.logLoginAttempt(0, null) } returns Unit

        // After dropFailedAttempts, lockoutManager should report clean state
        every { lockoutManager.currentState } returns LockoutState.Unlocked(null)

        // Enter correct PIN (6 digits)
        for (digit in "123456".map { it.digitToInt() }) {
            viewModel.onKeyClick(digit)
        }

        // App unlocks, then re-locks — unlocked() is called to reset the screen
        viewModel.unlocked()

        // The warning should be gone — attemptsLeft must be null
        val inputState = viewModel.uiState.inputState
        assert(inputState is PinUnlockModule.InputState.Enabled) {
            "Expected Enabled state, got $inputState"
        }
        assertNull(
            "attemptsLeft should be null after successful unlock and re-lock",
            (inputState as PinUnlockModule.InputState.Enabled).attemptsLeft
        )
    }
}
