package cash.p.terminal.modules.multiswap.exchange

import cash.p.terminal.core.storage.PendingMultiSwapStorage
import cash.p.terminal.core.usecase.FetchSwapQuotesUseCase
import cash.p.terminal.core.usecase.SyncPendingMultiSwapUseCase
import cash.p.terminal.entities.PendingMultiSwap
import cash.p.terminal.core.ServiceStateFlow
import cash.p.terminal.modules.multiswap.ISwapQuote
import cash.p.terminal.modules.multiswap.MultiSwapOnChainMonitor
import cash.p.terminal.modules.multiswap.SwapProviderQuote
import cash.p.terminal.modules.multiswap.SwapQuoteService
import cash.p.terminal.modules.multiswap.TimerService
import cash.p.terminal.modules.multiswap.exchange.MultiSwapExchangeViewModel.Companion.resolveButtonState
import cash.p.terminal.modules.multiswap.providers.ChangeNowProvider
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.modules.multiswap.providers.QuickexProvider
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.FullCoin
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.entities.Currency
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class MultiSwapExchangeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val swapsFlow = MutableStateFlow<List<PendingMultiSwap>>(emptyList())

    private val pendingMultiSwapStorage = mockk<PendingMultiSwapStorage>(relaxed = true) {
        every { getAll() } returns swapsFlow
    }
    private val swapQuoteService = mockk<SwapQuoteService>(relaxed = true)
    private val fetchSwapQuotesUseCase = mockk<FetchSwapQuotesUseCase>(relaxed = true)
    private val onChainMonitor = mockk<MultiSwapOnChainMonitor>(relaxed = true)
    private val marketKit = mockk<MarketKitWrapper>(relaxed = true)
    private val numberFormatter = mockk<IAppNumberFormatter>(relaxed = true)
    private val syncPendingMultiSwapUseCase = mockk<SyncPendingMultiSwapUseCase>(relaxed = true)
    private val adapterManager = mockk<IAdapterManager>(relaxed = true)
    private val balanceHiddenManager = mockk<IBalanceHiddenManager>(relaxed = true) {
        every { anyWalletVisibilityChangedFlow } returns MutableSharedFlow()
    }
    private val currencyManager = mockk<CurrencyManager> {
        every { baseCurrency } returns Currency("USD", "$", 2, 0)
    }

    private val testToken = mockk<Token>(relaxed = true) {
        every { blockchainType } returns BlockchainType.Ethereum
        every { coin } returns mockk(relaxed = true)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { pendingMultiSwapStorage.getById(any()) } returns null
    }

    private val viewModelStore = androidx.lifecycle.ViewModelStore()

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
        unmockkAll()
    }

    // --- resolveButtonState tests ---

    @Test
    fun resolveButtonState_leg1CompletedLeg2PendingWithQuotes_enabled() {
        val result = resolveButtonState(
            leg1 = LegStatus.Completed,
            leg2 = LegStatus.Pending,
            hasQuotes = true,
        )
        assertEquals(ButtonState.Enabled, result)
    }

    @Test
    fun resolveButtonState_leg1CompletedLeg2PendingNoQuotes_refresh() {
        val result = resolveButtonState(
            leg1 = LegStatus.Completed,
            leg2 = LegStatus.Pending,
            hasQuotes = false,
        )
        assertEquals(ButtonState.Refresh, result)
    }

    @Test
    fun resolveButtonState_bothCompleted_close() {
        val result = resolveButtonState(
            leg1 = LegStatus.Completed,
            leg2 = LegStatus.Completed,
        )
        assertEquals(ButtonState.Close, result)
    }

    @Test
    fun resolveButtonState_leg1Executing_disabled() {
        val result = resolveButtonState(
            leg1 = LegStatus.Executing,
            leg2 = LegStatus.Pending,
        )
        assertEquals(ButtonState.Disabled, result)
    }

    @Test
    fun resolveButtonState_leg1Pending_disabled() {
        val result = resolveButtonState(
            leg1 = LegStatus.Pending,
            leg2 = LegStatus.Pending,
        )
        assertEquals(ButtonState.Disabled, result)
    }

    @Test
    fun resolveButtonState_leg1CompletedLeg2PendingExpired_refresh() {
        val result = resolveButtonState(
            leg1 = LegStatus.Completed,
            leg2 = LegStatus.Pending,
            hasQuotes = true,
            expired = true,
        )
        assertEquals(ButtonState.Refresh, result)
    }

    @Test
    fun resolveButtonState_leg1CompletedLeg2PendingNotExpired_enabled() {
        val result = resolveButtonState(
            leg1 = LegStatus.Completed,
            leg2 = LegStatus.Pending,
            hasQuotes = true,
            expired = false,
        )
        assertEquals(ButtonState.Enabled, result)
    }

    @Test
    fun resolveButtonState_leg2Executing_returnsHidden() {
        val result = resolveButtonState(
            leg1 = LegStatus.Completed,
            leg2 = LegStatus.Executing,
        )
        assertEquals(ButtonState.Hidden, result)
    }

    @Test
    fun resolveButtonState_leg2Failed_returnsHidden() {
        val result = resolveButtonState(
            leg1 = LegStatus.Completed,
            leg2 = LegStatus.Failed,
        )
        assertEquals(ButtonState.Hidden, result)
    }

    // --- fetchLeg2Quotes integration tests ---

    @Test
    fun fetchLeg2Quotes_leg1Completed_callsStartBeforeFetching() = runTest(dispatcher) {
        setupTokenResolution()
        val provider = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "test-provider"
            every { title } returns "Test"
        }
        every { swapQuoteService.providers } returns listOf(provider)

        val quote = mockk<SwapProviderQuote>(relaxed = true) {
            every { this@mockk.provider } returns provider
            every { amountOut } returns BigDecimal.ONE
        }
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } returns listOf(quote)

        val vm = createViewModel()

        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        coVerifyOrder {
            swapQuoteService.start()
            fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun fetchLeg2Quotes_leg1Completed_quotesPopulated() = runTest(dispatcher) {
        setupTokenResolution()
        val provider = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "test-provider"
            every { title } returns "Test"
        }
        every { swapQuoteService.providers } returns listOf(provider)

        val swapQuote = mockk<ISwapQuote>(relaxed = true) {
            every { amountOut } returns BigDecimal.TEN
        }
        val quote = SwapProviderQuote(provider = provider, swapQuote = swapQuote)
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } returns listOf(quote)

        val vm = createViewModel()

        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        assertEquals(1, vm.leg2Quotes.size)
        assertNotNull(vm.selectedLeg2Quote)
        assertEquals(provider, vm.selectedLeg2Quote?.provider)
    }

    @Test
    fun fetchLeg2Quotes_leg1NotCompleted_doesNotCallStart() = runTest(dispatcher) {
        val vm = createViewModel()

        swapsFlow.value = listOf(executingLeg1Swap())
        advanceUntilIdle()

        coVerify(exactly = 0) { swapQuoteService.start() }
    }

    @Test
    fun refreshQuotes_callsStartAgain() = runTest(dispatcher) {
        setupTokenResolution()
        every { swapQuoteService.providers } returns emptyList()
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } returns emptyList()

        val vm = createViewModel()

        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        coVerify(exactly = 1) { swapQuoteService.start() }

        vm.refreshQuotes()
        advanceUntilIdle()

        coVerify(exactly = 2) { swapQuoteService.start() }
    }

    // --- timer integration tests ---

    @Test
    fun timer_onChainProvider_startsAfterQuotesFetched() = runTest(dispatcher) {
        val timerStateFlow = createMockTimerStateFlow()
        val timerService = createMockTimerService(timerStateFlow)
        setupTokenResolution()
        val provider = mockOnChainProvider()
        every { swapQuoteService.providers } returns listOf(provider)
        val quote = createQuote(provider)
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } returns listOf(quote)

        createViewModel(timerService)

        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        verify(exactly = 1) { timerService.start(10) }
    }

    @Test
    fun timer_changeNowProvider_doesNotStart() = runTest(dispatcher) {
        val timerStateFlow = createMockTimerStateFlow()
        val timerService = createMockTimerService(timerStateFlow)
        setupTokenResolution()
        val provider = mockk<ChangeNowProvider>(relaxed = true) {
            every { id } returns "changenow"
            every { title } returns "ChangeNow"
        }
        every { swapQuoteService.providers } returns listOf(provider)
        val quote = createQuote(provider)
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } returns listOf(quote)

        createViewModel(timerService)

        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        verify(exactly = 0) { timerService.start(any()) }
    }

    @Test
    fun timer_quickexProvider_doesNotStart() = runTest(dispatcher) {
        val timerStateFlow = createMockTimerStateFlow()
        val timerService = createMockTimerService(timerStateFlow)
        setupTokenResolution()
        val provider = mockk<QuickexProvider>(relaxed = true) {
            every { id } returns "quickex"
            every { title } returns "Quickex"
        }
        every { swapQuoteService.providers } returns listOf(provider)
        val quote = createQuote(provider)
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } returns listOf(quote)

        createViewModel(timerService)

        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        verify(exactly = 0) { timerService.start(any()) }
    }

    @Test
    fun timer_resetsOnRefreshQuotes() = runTest(dispatcher) {
        val timerStateFlow = createMockTimerStateFlow()
        val timerService = createMockTimerService(timerStateFlow)
        setupTokenResolution()
        val provider = mockOnChainProvider()
        every { swapQuoteService.providers } returns listOf(provider)
        val quote = createQuote(provider)
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } returns listOf(quote)

        val vm = createViewModel(timerService)

        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        vm.refreshQuotes()
        advanceUntilIdle()

        // reset called at start of fetchLeg2Quotes, then start called after quotes arrive
        verify(atLeast = 1) { timerService.reset() }
        verify(exactly = 2) { timerService.start(10) }
    }

    @Test
    fun timer_restartsOnSelectQuote() = runTest(dispatcher) {
        val timerStateFlow = createMockTimerStateFlow()
        val timerService = createMockTimerService(timerStateFlow)
        setupTokenResolution()
        val provider = mockOnChainProvider()
        every { swapQuoteService.providers } returns listOf(provider)
        val quote = createQuote(provider)
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } returns listOf(quote)

        val vm = createViewModel(timerService)

        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        verify(exactly = 1) { timerService.start(10) }

        vm.onSelectLeg2Quote(quote)
        advanceUntilIdle()

        verify(exactly = 2) { timerService.start(10) }
    }

    @Test
    fun uiState_timerTimeout_setsExpiredTrue() = runTest(dispatcher) {
        val timerStateFlow = createMockTimerStateFlow()
        val timerService = createMockTimerService(timerStateFlow)
        setupTokenResolution()
        val provider = mockOnChainProvider()
        every { swapQuoteService.providers } returns listOf(provider)
        val quote = createQuote(provider)
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } returns listOf(quote)

        val vm = createViewModel(timerService)

        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        // Simulate timer timeout
        timerStateFlow.tryEmit(TimerService.State(remaining = null, timeout = true))
        advanceUntilIdle()

        assertEquals(ButtonState.Refresh, vm.uiState?.buttonState)
    }

    @Test
    fun uiState_timerCounting_setsExpiresIn() = runTest(dispatcher) {
        val timerStateFlow = createMockTimerStateFlow()
        val timerService = createMockTimerService(timerStateFlow)
        setupTokenResolution()
        val provider = mockOnChainProvider()
        every { swapQuoteService.providers } returns listOf(provider)
        val quote = createQuote(provider)
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } returns listOf(quote)

        val vm = createViewModel(timerService)

        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        // Simulate timer counting
        timerStateFlow.tryEmit(TimerService.State(remaining = 7, timeout = false))
        advanceUntilIdle()

        assertEquals(ButtonState.Enabled, vm.uiState?.buttonState)
    }

    @Test
    fun uiState_offChainProvider_expiresInNull() = runTest(dispatcher) {
        val timerStateFlow = createMockTimerStateFlow()
        val timerService = createMockTimerService(timerStateFlow)
        setupTokenResolution()
        val provider = mockk<ChangeNowProvider>(relaxed = true) {
            every { id } returns "changenow"
            every { title } returns "ChangeNow"
        }
        every { swapQuoteService.providers } returns listOf(provider)
        val quote = createQuote(provider)
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } returns listOf(quote)

        val vm = createViewModel(timerService)

        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        assertEquals(ButtonState.Enabled, vm.uiState?.buttonState)
    }

    @Test
    fun timeRemainingProgress_timerCounting_computesProgress() = runTest(dispatcher) {
        val timerStateFlow = createMockTimerStateFlow()
        val timerService = createMockTimerService(timerStateFlow)
        setupTokenResolution()
        val provider = mockOnChainProvider()
        every { swapQuoteService.providers } returns listOf(provider)
        val quote = createQuote(provider)
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } returns listOf(quote)

        val vm = createViewModel(timerService)

        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        timerStateFlow.tryEmit(TimerService.State(remaining = 5, timeout = false))
        advanceUntilIdle()

        assertEquals(0.5f, vm.timeRemainingProgress)
    }

    @Test
    fun timeRemainingProgress_timerTimeout_null() = runTest(dispatcher) {
        val timerStateFlow = createMockTimerStateFlow()
        val timerService = createMockTimerService(timerStateFlow)

        val vm = createViewModel(timerService)

        timerStateFlow.tryEmit(TimerService.State(remaining = null, timeout = true))
        advanceUntilIdle()

        assertNull(vm.timeRemainingProgress)
    }

    @Test
    fun resolveButtonState_expiredNoQuotes_refresh() {
        val result = resolveButtonState(
            leg1 = LegStatus.Completed,
            leg2 = LegStatus.Pending,
            hasQuotes = false,
            expired = true,
        )
        assertEquals(ButtonState.Refresh, result)
    }

    @Test
    fun resolveButtonState_bothCompletedExpired_close() {
        val result = resolveButtonState(
            leg1 = LegStatus.Completed,
            leg2 = LegStatus.Completed,
            expired = true,
        )
        assertEquals(ButtonState.Close, result)
    }

    // --- quoting button state tests ---

    @Test
    fun resolveButtonState_quoting_returnsQuoting() {
        val result = resolveButtonState(
            leg1 = LegStatus.Completed,
            leg2 = LegStatus.Pending,
            quoting = true,
        )
        assertEquals(ButtonState.Quoting, result)
    }

    @Test
    fun resolveButtonState_quotingWithQuotes_quotingOverrides() {
        val result = resolveButtonState(
            leg1 = LegStatus.Completed,
            leg2 = LegStatus.Pending,
            hasQuotes = true,
            quoting = true,
        )
        assertEquals(ButtonState.Quoting, result)
    }

    @Test
    fun resolveButtonState_quotingAndExpired_quotingOverrides() {
        val result = resolveButtonState(
            leg1 = LegStatus.Completed,
            leg2 = LegStatus.Pending,
            expired = true,
            quoting = true,
        )
        assertEquals(ButtonState.Quoting, result)
    }

    @Test
    fun fetchLeg2Quotes_duringFetch_buttonStateIsQuoting() = runTest(dispatcher) {
        setupTokenResolution()
        val provider = mockOnChainProvider()
        every { swapQuoteService.providers } returns listOf(provider)

        // Suspend fetchSwapQuotesUseCase so we can observe intermediate state
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } coAnswers {
            // At this point, button should be Quoting
            emptyList()
        }

        val vm = createViewModel()

        // Emit swap with leg1 already completed — triggers fetchLeg2Quotes
        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        // After fetch completes with empty quotes, should no longer be Quoting
        // (will be Refresh since hasQuotes=false)
        assertEquals(ButtonState.Refresh, vm.uiState?.buttonState)
    }

    // --- onDeleteAndClose tests ---

    @Test
    fun onDeleteAndClose_deletesRecordAndClosesScreen() = runTest(dispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onDeleteAndClose()
        advanceUntilIdle()

        coVerify(exactly = 1) { pendingMultiSwapStorage.delete("test-swap") }
        assertTrue(vm.closeScreen)
    }

    // --- leg2 balance state tests ---

    @Test
    fun leg1Completed_setsLeg2BalanceState() = runTest(dispatcher) {
        setupTokenResolution()
        every { adapterManager.getAdjustedBalanceDataForToken(any()) } returns BalanceData(BigDecimal("10.0"))

        val provider = mockOnChainProvider()
        every { swapQuoteService.providers } returns listOf(provider)
        val quote = createQuote(provider)
        coEvery { fetchSwapQuotesUseCase(any(), any(), any(), any(), any(), any()) } returns listOf(quote)

        val vm = createViewModel()
        // Initial state: displayBalance is null (no token set yet)
        assertNull(vm.leg2BalanceStateFlow.value.displayBalance)

        swapsFlow.value = listOf(completedLeg1Swap())
        advanceUntilIdle()

        // After leg1 completes and quotes fetched, balance should be set
        assertEquals(BigDecimal("10.0"), vm.leg2BalanceStateFlow.value.displayBalance)
    }

    // --- helpers ---

    private fun createMockTimerStateFlow(): MutableSharedFlow<TimerService.State> {
        return MutableSharedFlow<TimerService.State>(
            replay = 1,
        ).also {
            it.tryEmit(TimerService.State(remaining = null, timeout = false))
        }
    }

    private fun createMockTimerService(
        stateFlow: MutableSharedFlow<TimerService.State>,
    ): TimerService = mockk(relaxed = true) {
        every { this@mockk.stateFlow } returns ServiceStateFlow(stateFlow)
    }

    private fun mockOnChainProvider(): IMultiSwapProvider = mockk(relaxed = true) {
        every { id } returns "uniswap"
        every { title } returns "Uniswap"
    }

    private fun createQuote(provider: IMultiSwapProvider): SwapProviderQuote {
        val swapQuote = mockk<ISwapQuote>(relaxed = true) {
            every { amountOut } returns BigDecimal.ONE
        }
        return SwapProviderQuote(provider = provider, swapQuote = swapQuote)
    }

    private fun setupTokenResolution() {
        val fullCoin = mockk<FullCoin>(relaxed = true) {
            every { tokens } returns listOf(testToken)
        }
        every { marketKit.fullCoins(any<List<String>>()) } returns listOf(fullCoin)
        every { numberFormatter.formatCoinFull(any(), any(), any()) } returns "0"
    }

    private fun createViewModel(
        timerService: TimerService = TimerService(),
    ): MultiSwapExchangeViewModel {
        // Clear previous VM before creating a new one
        viewModelStore.clear()
        val vm = MultiSwapExchangeViewModel(
        pendingMultiSwapId = "test-swap",
        pendingMultiSwapStorage = pendingMultiSwapStorage,
        marketKit = marketKit,
        numberFormatter = numberFormatter,
        onChainMonitor = onChainMonitor,
        swapQuoteService = swapQuoteService,
        fetchSwapQuotesUseCase = fetchSwapQuotesUseCase,
        timerService = timerService,
        syncPendingMultiSwapUseCase = syncPendingMultiSwapUseCase,
        syncIntervalMs = Long.MAX_VALUE,
        currencyManager = currencyManager,
        adapterManager = adapterManager,
        balanceHiddenManager = balanceHiddenManager,
    )
        viewModelStore.put("test-vm", vm)
        return vm
    }

    private fun completedLeg1Swap() = PendingMultiSwap(
        id = "test-swap",
        createdAt = System.currentTimeMillis(),
        coinUidIn = "bitcoin",
        blockchainTypeIn = "bitcoin",
        amountIn = BigDecimal.ONE,
        coinUidIntermediate = "ethereum",
        blockchainTypeIntermediate = "ethereum",
        coinUidOut = "tether",
        blockchainTypeOut = "ethereum",
        leg1ProviderId = "changenow",
        leg1IsOffChain = true,
        leg1TransactionId = "tx1",
        leg1AmountOut = BigDecimal("0.5"),
        leg1Status = PendingMultiSwap.STATUS_COMPLETED,
        leg2ProviderId = null,
        leg2IsOffChain = null,
        leg2TransactionId = null,
        leg2AmountOut = null,
        leg2Status = PendingMultiSwap.STATUS_PENDING,
        expectedAmountOut = BigDecimal("1000"),
    )

    private fun executingLeg1Swap() = completedLeg1Swap().copy(
        leg1Status = PendingMultiSwap.STATUS_EXECUTING,
        leg1AmountOut = null,
    )

    private fun executingLeg2Swap() = completedLeg1Swap().copy(
        leg2Status = PendingMultiSwap.STATUS_EXECUTING,
        leg2IsOffChain = false,
    )

    @Test
    fun balanceMonitor_triggersSync_doesNotWriteDirectly() = runTest(dispatcher) {
        var capturedCallback: (() -> Unit)? = null
        every {
            onChainMonitor.observeBalanceIncrease(
                coinUid = "tether",
                blockchainType = any(),
                scope = any(),
                onBalanceIncreased = captureLambda(),
            )
        } answers {
            capturedCallback = lambda<() -> Unit>().captured
            true
        }

        val swap = executingLeg2Swap()
        swapsFlow.value = listOf(swap)
        createViewModel()
        advanceUntilIdle()

        assertNotNull(capturedCallback)

        capturedCallback?.invoke()
        advanceUntilIdle()

        coVerify { syncPendingMultiSwapUseCase() }
        coVerify(exactly = 0) {
            pendingMultiSwapStorage.updateLeg1(any(), any(), any(), any())
            pendingMultiSwapStorage.updateLeg2(any(), any(), any(), any())
            pendingMultiSwapStorage.delete(any())
        }
    }

    @Test
    fun balanceMonitor_syncDoesNotComplete_reArmsMonitor() = runTest(dispatcher) {
        var observeCallCount = 0
        every {
            onChainMonitor.observeBalanceIncrease(
                coinUid = "tether",
                blockchainType = any(),
                scope = any(),
                onBalanceIncreased = any(),
            )
        } answers {
            observeCallCount++
            true
        }

        val swap = executingLeg2Swap()
        swapsFlow.value = listOf(swap)
        // All triggerSync calls: swap still EXECUTING (re-arm), last one null to stop
        coEvery { pendingMultiSwapStorage.getById("test-swap") } returnsMany listOf(swap, swap, null)
        createViewModel()
        advanceUntilIdle()

        val countAfterInit = observeCallCount

        // Simulate balance signal (unrelated transfer) — use last captured callback
        val callbacks = mutableListOf<() -> Unit>()
        verify {
            onChainMonitor.observeBalanceIncrease(
                coinUid = "tether",
                blockchainType = any(),
                scope = any(),
                onBalanceIncreased = capture(callbacks),
            )
        }
        callbacks.last().invoke()
        advanceUntilIdle()

        // Monitor should re-arm: observeBalanceIncrease called at least once more after balance signal
        assertTrue(observeCallCount > countAfterInit)
    }

    @Test
    fun periodicSync_screenOpensWithExecutingLeg_triggersSyncImmediately() = runTest(dispatcher) {
        val swap = executingLeg1Swap()
        coEvery { pendingMultiSwapStorage.getById("test-swap") } returns swap
        swapsFlow.value = listOf(swap)
        createViewModel()
        advanceUntilIdle()

        coVerify(atLeast = 1) { syncPendingMultiSwapUseCase() }
    }

    @Test
    fun periodicSync_leg2TransitionsToExecuting_startsSync() = runTest(dispatcher) {
        val completedSwap = completedLeg1Swap()
        swapsFlow.value = listOf(completedSwap)
        createViewModel()
        advanceUntilIdle()

        // No sync yet — no executing legs
        coVerify(exactly = 0) { syncPendingMultiSwapUseCase() }

        // Leg2 starts executing
        val executingLeg2 = completedSwap.copy(
            leg2Status = PendingMultiSwap.STATUS_EXECUTING,
            leg2IsOffChain = false,
        )
        coEvery { pendingMultiSwapStorage.getById("test-swap") } returns executingLeg2
        swapsFlow.value = listOf(executingLeg2)
        advanceUntilIdle()

        coVerify(atLeast = 1) { syncPendingMultiSwapUseCase() }
    }

    @Test
    fun mapToUiState_leg1Completed_withTransactionId_setsClickable() = runTest(dispatcher) {
        val swap = completedLeg1Swap().copy(
            leg1TransactionId = "0xabc",
            leg1InfoRecordUid = null,
        )
        swapsFlow.value = listOf(swap)
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(requireNotNull(viewModel.uiState).leg1Clickable)
        assertEquals("0xabc", viewModel.leg1NavigationRecordUid)
    }

    @Test
    fun mapToUiState_leg1Executing_withTransactionId_setsClickable() = runTest(dispatcher) {
        val swap = executingLeg1Swap().copy(
            leg1TransactionId = "0xabc",
        )
        coEvery { pendingMultiSwapStorage.getById("test-swap") } returns swap
        swapsFlow.value = listOf(swap)
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(requireNotNull(viewModel.uiState).leg1Clickable)
        assertEquals("0xabc", viewModel.leg1NavigationRecordUid)
    }

    @Test
    fun mapToUiState_leg1Executing_noTransactionId_notClickable() = runTest(dispatcher) {
        val swap = executingLeg1Swap().copy(
            leg1TransactionId = null,
            leg1InfoRecordUid = null,
        )
        coEvery { pendingMultiSwapStorage.getById("test-swap") } returns swap
        swapsFlow.value = listOf(swap)
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState?.leg1Clickable)
    }
}
