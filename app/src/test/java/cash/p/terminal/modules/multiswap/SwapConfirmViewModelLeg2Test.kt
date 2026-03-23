package cash.p.terminal.modules.multiswap

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ServiceStateFlow
import cash.p.terminal.core.storage.PendingMultiSwapStorage
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.modules.multiswap.sendtransaction.ISendTransactionService
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionResult
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionServiceState
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.WalletFactory
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SwapConfirmViewModelLeg2Test {

    private val dispatcher = UnconfinedTestDispatcher()
    private val pendingMultiSwapStorage = mockk<PendingMultiSwapStorage>(relaxed = true)
    private val localStorage = mockk<ILocalStorage>(relaxed = true)
    private val sendResult = mockk<SendTransactionResult>(relaxed = true)

    private val previewWallet = WalletFactory.previewWallet()
    private val token: Token = previewWallet.token
    private val swapQuote = mockk<ISwapQuote>(relaxed = true) {
        every { tokenIn } returns token
        every { tokenOut } returns token
        every { amountIn } returns BigDecimal.ONE
    }
    private val provider = mockk<IMultiSwapProvider>(relaxed = true) {
        every { mevProtectionAvailable } returns false
        // Suspend forever so fetchFinalQuote's Dispatchers.IO coroutine doesn't complete
        // and leak cancellation exceptions on VM cleanup
        coEvery { fetchFinalQuote(any(), any(), any(), any(), any(), any()) } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }
    }

    private val sendTransactionServiceState = SendTransactionServiceState(
        availableBalance = null,
        networkFee = null,
        cautions = emptyList(),
        sendable = true,
        loading = false,
        fields = emptyList(),
        extraFees = emptyMap(),
    )
    private val sendTransactionService = mockk<ISendTransactionService<Nothing>>(relaxed = true) {
        every { hasSettings() } returns false
        every { mevProtectionAvailable } returns false
        every { stateFlow } returns ServiceStateFlow(
            MutableSharedFlow<SendTransactionServiceState>(
                replay = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            ).also { it.tryEmit(sendTransactionServiceState) }.asSharedFlow()
        )
        every { sendTransactionSettingsFlow } returns MutableStateFlow(SendTransactionSettings.Common)
        coEvery { sendTransaction(any()) } returns sendResult
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        startKoin {
            modules(module {
                single<ILocalStorage> { localStorage }
                single<PendingMultiSwapStorage> { pendingMultiSwapStorage }
                single<MarketKitWrapper> { mockk(relaxed = true) }
                single<IBalanceHiddenManager> {
                    mockk(relaxed = true) {
                        every { balanceHiddenFlow } returns MutableStateFlow(false)
                    }
                }
            })
        }
    }

    private val viewModelStore = androidx.lifecycle.ViewModelStore()

    @After
    fun tearDown() {
        viewModelStore.clear()
        dispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
        stopKoin()
        unmockkAll()
    }

    @Test
    fun handleMultiSwapCompletion_leg2_deletesPendingSwap() = runTest(dispatcher) {
        val legInfo = MultiSwapLegInfo.Leg2(pendingMultiSwapId = "swap-123")
        val vm = createViewModel(legInfo)
        advanceUntilIdle()

        vm.onClickSendWithWarningCheck()
        advanceUntilIdle()

        coVerify(exactly = 1) { pendingMultiSwapStorage.delete("swap-123") }
        assertEquals("swap-123", vm.completedMultiSwapId)

        // Clear VM inside runTest so coroutine cancellations are handled by the test dispatcher
        viewModelStore.clear()
        advanceUntilIdle()
    }

    private fun createViewModel(legInfo: MultiSwapLegInfo): SwapConfirmViewModel {
        val adapterManager = mockk<IAdapterManager>(relaxed = true)
        val marketKit = mockk<MarketKitWrapper>(relaxed = true)
        val currencyManager = mockk<CurrencyManager> {
            every { baseCurrency } returns Currency("USD", "$", 2, 0)
        }
        val vm = SwapConfirmViewModel(
            swapProvider = provider,
            swapQuote = swapQuote,
            swapSettings = emptyMap(),
            currencyManager = currencyManager,
            fiatServiceIn = FiatService(marketKit),
            fiatServiceOut = FiatService(marketKit),
            fiatServiceOutMin = FiatService(marketKit),
            sendTransactionService = sendTransactionService,
            timerService = TimerService(),
            priceImpactService = PriceImpactService(),
            wallet = previewWallet,
            adapterManager = adapterManager,
            multiSwapLegInfo = legInfo,
        )
        viewModelStore.put("test-vm", vm)
        return vm
    }
}
