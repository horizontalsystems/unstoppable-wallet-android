package cash.p.terminal.modules.main

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.managers.DAppRequestEntityWrapper
import cash.p.terminal.core.managers.DefaultUserManager
import cash.p.terminal.core.managers.TonConnectManager
import cash.p.terminal.modules.calculator.domain.CalculatorModeService
import cash.p.terminal.premium.domain.usecase.CheckPremiumUseCase
import cash.p.terminal.premium.domain.usecase.PremiumType
import cash.p.terminal.wallet.IAccountManager
import io.horizontalsystems.core.ILoginRecordRepository
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.entities.AutoDeletePeriod
import io.horizontalsystems.tonkit.models.SignTransaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)
    private val dispatcherProvider = TestDispatcherProvider(dispatcher, testScope)

    private val accountManager = mockk<IAccountManager>(relaxed = true)
    private val systemInfoManager = mockk<ISystemInfoManager>(relaxed = true)
    private val localStorage = mockk<ILocalStorage>(relaxed = true)
    private val checkPremiumUseCase = mockk<CheckPremiumUseCase>()
    private val calculatorModeService = mockk<CalculatorModeService>(relaxed = true)
    private val pinComponent = mockk<IPinComponent>()
    private val keyStoreManager = mockk<IKeyStoreManager>(relaxed = true)
    private val tonConnectManager = mockk<TonConnectManager>()
    private val loginRecordRepository = mockk<ILoginRecordRepository>(relaxed = true)

    private lateinit var userManager: DefaultUserManager
    private lateinit var isLockedFlow: MutableStateFlow<Boolean>
    private lateinit var sendRequestFlow: MutableSharedFlow<SignTransaction>
    private lateinit var dappRequestFlow: MutableSharedFlow<DAppRequestEntityWrapper>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        userManager = DefaultUserManager(accountManager)
        isLockedFlow = MutableStateFlow(true)
        sendRequestFlow = MutableSharedFlow()
        dappRequestFlow = MutableSharedFlow()

        every { pinComponent.isLockedFlow } returns isLockedFlow
        every { localStorage.getAutoDeleteLogsPeriod(any()) } returns AutoDeletePeriod.NEVER.value
        every { localStorage.isCalculatorModeEnabled } returns false
        every { tonConnectManager.sendRequestFlow } returns sendRequestFlow
        every { tonConnectManager.dappRequestFlow } returns dappRequestFlow
        coEvery { checkPremiumUseCase.update() } returns PremiumType.NONE
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun currentUserLevelFlow_sameLevelUnlock_refreshesAgain() = runTest(dispatcher) {
        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 0) { checkPremiumUseCase.update() }

        userManager.setUserLevel(0)
        advanceUntilIdle()

        coVerify(exactly = 0) { checkPremiumUseCase.update() }

        isLockedFlow.value = false
        advanceUntilIdle()

        coVerify(exactly = 1) { checkPremiumUseCase.update() }

        isLockedFlow.value = true
        advanceUntilIdle()

        isLockedFlow.value = false
        advanceUntilIdle()

        coVerify(exactly = 2) { checkPremiumUseCase.update() }
    }

    private fun createViewModel() = MainActivityViewModel(
        userManager = userManager,
        accountManager = accountManager,
        systemInfoManager = systemInfoManager,
        localStorage = localStorage,
        checkPremiumUseCase = checkPremiumUseCase,
        calculatorModeService = calculatorModeService,
        dispatcherProvider = dispatcherProvider,
        pinComponent = pinComponent,
        keyStoreManager = keyStoreManager,
        tonConnectManager = tonConnectManager,
        loginRecordRepository = loginRecordRepository,
    )
}
