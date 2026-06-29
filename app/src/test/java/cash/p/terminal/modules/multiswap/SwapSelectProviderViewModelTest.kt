package cash.p.terminal.modules.multiswap

import cash.p.terminal.core.App
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.entities.Currency
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SwapSelectProviderViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val testCoin = mockk<Coin> {
        every { uid } returns "btc"
        every { code } returns "BTC"
    }
    private val testToken = mockk<Token> {
        every { coin } returns testCoin
        every { decimals } returns 8
    }

    private val marketKit = mockk<MarketKitWrapper>(relaxed = true) {
        every { coinPrice(any(), any()) } returns null
        every { coinPriceObservable(any(), any(), any()) } returns Observable.never()
    }
    private val numberFormatter = mockk<IAppNumberFormatter>(relaxed = true) {
        every { formatCoinFull(any(), any(), any()) } returns "0"
    }
    private val currencyManager = mockk<CurrencyManager> {
        every { baseCurrency } returns Currency("USD", "$", 2, 0)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(App)
        every { App.currencyManager } returns currencyManager
        every { App.marketKit } returns marketKit
        every { App.numberFormatter } returns numberFormatter
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun sorted_bestTime_ordersByEstimationTimeAscendingNullLast() {
        val viewModel = SwapSelectProviderViewModel(
            listOf(
                quote(providerId = "slow", amount = "100", eta = 600L),
                quote(providerId = "null", amount = "100", eta = null),
                quote(providerId = "fast", amount = "100", eta = 120L),
            )
        )

        viewModel.setSortType(ProviderSortType.BestTime)

        assertEquals(listOf("fast", "slow", "null"), viewModel.providerIds())
    }

    @Test
    fun sorted_bestPrice_ordersByAmountOutDescThenEstimationTime() {
        val viewModel = SwapSelectProviderViewModel(
            listOf(
                quote(providerId = "slowSameAmount", amount = "100", eta = 600L),
                quote(providerId = "fastSameAmount", amount = "100", eta = 120L),
                quote(providerId = "bestAmount", amount = "200", eta = null),
            )
        )

        // BestPrice is the default sort type — highest amountOut first, estimationTime as tie-breaker.
        assertEquals(
            listOf("bestAmount", "fastSameAmount", "slowSameAmount"),
            viewModel.providerIds()
        )
    }

    @Test
    fun swapRates_bestTime_preservesOrder() {
        val viewModel = SwapSelectProviderViewModel(
            listOf(
                quote(providerId = "slow", amount = "100", eta = 600L),
                quote(providerId = "null", amount = "100", eta = null),
                quote(providerId = "fast", amount = "100", eta = 120L),
            )
        )
        viewModel.setSortType(ProviderSortType.BestTime)
        val orderBeforeToggle = viewModel.providerIds()

        viewModel.swapRates()

        assertEquals(orderBeforeToggle, viewModel.providerIds())
        assertEquals(listOf("fast", "slow", "null"), viewModel.providerIds())
    }

    private fun SwapSelectProviderViewModel.providerIds(): List<String> =
        uiState.quoteViewItems.map { it.quote.provider.id }

    private fun quote(providerId: String, amount: String, eta: Long?): SwapProviderQuote {
        val provider = mockk<IMultiSwapProvider> {
            every { id } returns providerId
        }
        val swapQuote = mockk<ISwapQuote> {
            every { tokenIn } returns testToken
            every { tokenOut } returns testToken
            every { amountIn } returns BigDecimal.ONE
            every { amountOut } returns BigDecimal(amount)
            every { estimationTime } returns eta
        }
        return SwapProviderQuote(provider, swapQuote)
    }
}
