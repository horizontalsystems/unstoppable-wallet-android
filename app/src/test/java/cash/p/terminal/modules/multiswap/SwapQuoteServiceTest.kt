package cash.p.terminal.modules.multiswap

import cash.p.terminal.core.usecase.FetchSwapQuotesUseCase
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import java.math.BigDecimal
import cash.p.terminal.modules.multiswap.providers.StonFiProvider
import cash.p.terminal.wallet.Token
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class SwapQuoteServiceTest {

    private val stonFiProvider = mockk<StonFiProvider>(relaxed = true)
    private val dispatcher = StandardTestDispatcher()

    private val tokenIn = mockk<Token>()
    private val tokenOut = mockk<Token>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        startKoin {
            modules(module { single { stonFiProvider } })
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
        unmockkAll()
    }

    @Test
    fun setTokens_allProvidersSlow_noSupportedSwapProviderError() = runTest(dispatcher) {
        val slowProvider = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "slow"
            coEvery { supports(tokenIn, tokenOut) } coAnswers {
                delay(6000)
                true
            }
        }

        val service = createService(listOf(slowProvider))
        service.setTokenIn(tokenIn)
        service.setTokenOut(tokenOut)
        service.setAmount(BigDecimal.ONE)
        advanceUntilIdle()

        val state = service.stateFlow.value
        assertTrue(state.error is NoSupportedSwapProvider)
    }

    @Test
    fun setTokens_fastProvider_noError() = runTest(dispatcher) {
        val fastProvider = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "fast"
            coEvery { supports(tokenIn, tokenOut) } returns true
        }

        val service = createService(listOf(fastProvider))
        service.setTokenIn(tokenIn)
        service.setTokenOut(tokenOut)
        advanceUntilIdle()

        val state = service.stateFlow.value
        assertNull(state.error)
    }

    @Test
    fun setTokens_slowProviderSkipped_fastProviderKept() = runTest(dispatcher) {
        val fastProvider = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "fast"
            coEvery { supports(tokenIn, tokenOut) } returns true
        }
        val slowProvider = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "slow"
            coEvery { supports(tokenIn, tokenOut) } coAnswers {
                delay(6000)
                true
            }
        }

        val service = createService(listOf(fastProvider, slowProvider))
        service.setTokenIn(tokenIn)
        service.setTokenOut(tokenOut)
        advanceUntilIdle()

        val state = service.stateFlow.value
        // fast provider passed supports() → no error, service found at least one provider
        assertNull(state.error)
    }

    @Test
    fun setTokens_providerThrows_excluded() = runTest(dispatcher) {
        val failingProvider = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "failing"
            coEvery { supports(tokenIn, tokenOut) } throws RuntimeException("network error")
        }

        val service = createService(listOf(failingProvider))
        service.setTokenIn(tokenIn)
        service.setTokenOut(tokenOut)
        service.setAmount(BigDecimal.ONE)
        advanceUntilIdle()

        val state = service.stateFlow.value
        assertTrue(state.error is NoSupportedSwapProvider)
    }

    @Test
    fun setTokens_unsupportedProvider_noSupportedError() = runTest(dispatcher) {
        val unsupported = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "unsupported"
            coEvery { supports(tokenIn, tokenOut) } returns false
        }

        val service = createService(listOf(unsupported))
        service.setTokenIn(tokenIn)
        service.setTokenOut(tokenOut)
        service.setAmount(BigDecimal.ONE)
        advanceUntilIdle()

        val state = service.stateFlow.value
        assertTrue(state.error is NoSupportedSwapProvider)
    }

    @Test
    fun start_callsStartOnAllProviders() = runTest(dispatcher) {
        val provider1 = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "provider1"
        }
        val provider2 = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "provider2"
        }

        val service = createService(listOf(provider1, provider2))
        service.start()
        advanceUntilIdle()

        coVerify(exactly = 1) { provider1.start() }
        coVerify(exactly = 1) { provider2.start() }
    }

    @Test
    fun start_providerThrows_continuesWithOthers() = runTest(dispatcher) {
        val failingProvider = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "failing"
            coEvery { start() } throws RuntimeException("init failed")
        }
        val healthyProvider = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "healthy"
        }

        val service = createService(listOf(failingProvider, healthyProvider))
        service.start()
        advanceUntilIdle()

        coVerify(exactly = 1) { failingProvider.start() }
        coVerify(exactly = 1) { healthyProvider.start() }
    }

    private fun createService(providers: List<IMultiSwapProvider>): SwapQuoteService {
        val routeResolver = mockk<MultiSwapRouteResolver>(relaxed = true) {
            coEvery { findRoute(any(), any(), any(), any(), any()) } returns null
        }
        return SwapQuoteService(
            mockk(relaxed = true),
            mockk(relaxed = true),
            routeResolver,
            FetchSwapQuotesUseCase(),
        ).apply {
            allProviders = providers
            coroutineScope = CoroutineScope(dispatcher)
        }
    }
}
