package cash.p.terminal.core.usecase

import cash.p.terminal.modules.multiswap.ISwapQuote
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.wallet.Token
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class FetchSwapQuotesUseCaseTest {

    private val useCase = FetchSwapQuotesUseCase()
    private val tokenIn = mockk<Token>()
    private val tokenOut = mockk<Token>()
    private val amountIn = BigDecimal("1.0")

    private fun mockProvider(
        id: String,
        supports: Boolean = true,
        amountOut: BigDecimal = BigDecimal.ONE,
        priority: Int = 0,
    ): IMultiSwapProvider = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.priority } returns priority
        coEvery { supports(tokenIn, tokenOut) } returns supports
        coEvery { fetchQuote(tokenIn, tokenOut, amountIn, any()) } returns mockk<ISwapQuote> {
            every { this@mockk.amountOut } returns amountOut
        }
    }

    @Test
    fun happyPath_returnsSortedByPriorityThenAmount() = runTest {
        val lowPriority = mockProvider("low", amountOut = BigDecimal("10"), priority = 1)
        val highPriority = mockProvider("high", amountOut = BigDecimal("5"), priority = 2)

        val result = useCase(listOf(lowPriority, highPriority), tokenIn, tokenOut, amountIn)

        assertEquals(2, result.size)
        assertEquals("high", result[0].provider.id)
        assertEquals("low", result[1].provider.id)
    }

    @Test
    fun samePriority_sortedByAmountDescending() = runTest {
        val small = mockProvider("small", amountOut = BigDecimal("1"), priority = 1)
        val large = mockProvider("large", amountOut = BigDecimal("10"), priority = 1)

        val result = useCase(listOf(small, large), tokenIn, tokenOut, amountIn)

        assertEquals("large", result[0].provider.id)
        assertEquals("small", result[1].provider.id)
    }

    @Test
    fun unsupportedProvider_excluded() = runTest {
        val supported = mockProvider("ok", supports = true)
        val unsupported = mockProvider("no", supports = false)

        val result = useCase(listOf(supported, unsupported), tokenIn, tokenOut, amountIn)

        assertEquals(1, result.size)
        assertEquals("ok", result[0].provider.id)
    }

    @Test
    fun allUnsupported_emptyList() = runTest {
        val unsupported = mockProvider("no", supports = false)

        val result = useCase(listOf(unsupported), tokenIn, tokenOut, amountIn)

        assertTrue(result.isEmpty())
    }

    @Test
    fun providerSupportsThrows_excluded() = runTest {
        val failing = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "fail"
            coEvery { supports(tokenIn, tokenOut) } throws RuntimeException("network")
        }
        val ok = mockProvider("ok")

        val result = useCase(listOf(failing, ok), tokenIn, tokenOut, amountIn)

        assertEquals(1, result.size)
        assertEquals("ok", result[0].provider.id)
    }

    @Test
    fun providerFetchQuoteThrows_excluded() = runTest {
        val failing = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "fail"
            every { priority } returns 0
            coEvery { supports(tokenIn, tokenOut) } returns true
            coEvery { fetchQuote(tokenIn, tokenOut, amountIn, any()) } throws RuntimeException("error")
        }
        val ok = mockProvider("ok")

        val result = useCase(listOf(failing, ok), tokenIn, tokenOut, amountIn)

        assertEquals(1, result.size)
        assertEquals("ok", result[0].provider.id)
    }

    @Test
    fun providerSupportsTimeout_excluded() = runTest {
        val slow = mockk<IMultiSwapProvider>(relaxed = true) {
            every { id } returns "slow"
            coEvery { supports(tokenIn, tokenOut) } coAnswers {
                delay(10_000)
                true
            }
        }
        val fast = mockProvider("fast")

        val result = useCase(listOf(slow, fast), tokenIn, tokenOut, amountIn)

        assertEquals(1, result.size)
        assertEquals("fast", result[0].provider.id)
    }

    @Test
    fun onProviderError_callbackInvoked() = runTest {
        val error = RuntimeException("deposit too small")
        // Use a provider that supports the pair but throws on fetchQuote
        val ok = mockProvider("ok")
        val failing = mockProvider("fail")
        coEvery { failing.fetchQuote(tokenIn, tokenOut, amountIn, any()) } throws error

        val errors = mutableListOf<Pair<String, Throwable>>()
        val result = useCase(
            providers = listOf(ok, failing),
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            onProviderError = { provider, e -> errors.add(provider.id to e) },
        )

        assertEquals(1, result.size)
        assertEquals("ok", result[0].provider.id)
        assertEquals(1, errors.size)
        assertEquals("fail", errors[0].first)
        assertEquals(error.message, errors[0].second.message)
    }

    @Test
    fun emptyProvidersList_emptyResult() = runTest {
        val result = useCase(emptyList(), tokenIn, tokenOut, amountIn)
        assertTrue(result.isEmpty())
    }
}
