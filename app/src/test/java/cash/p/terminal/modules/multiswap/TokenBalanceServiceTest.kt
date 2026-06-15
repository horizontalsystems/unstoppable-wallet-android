package cash.p.terminal.modules.multiswap

import cash.p.terminal.core.ISendZcashAdapter
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TokenBalanceServiceTest {

    private val adapterManager = mockk<IAdapterManager>()
    private val marketKit = mockk<MarketKitWrapper>()
    private val adapter = mockk<ISendZcashAdapter>()
    private val balanceUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val feeFlow = MutableStateFlow(BigDecimal("0.0001"))
    private var adjustedBalanceData = BalanceData(available = BigDecimal("3.9999"))
    private var adapterBalanceData = BalanceData(available = BigDecimal("4.9999"))
    private val token = Token(
        coin = Coin(uid = "zcash", name = "Zcash", code = "ZEC"),
        blockchain = Blockchain(BlockchainType.Zcash, "Zcash", null),
        type = TokenType.AddressSpecTyped(TokenType.AddressSpecType.Shielded),
        decimals = 8
    )

    @Test
    fun setToken_zcashNativeWithLocalPending_usesFeeAdjustedAvailableBalance() {
        stubZcashBalance()

        val service = TokenBalanceService(adapterManager, marketKit)
        service.setToken(token)

        assertBigDecimalEquals("3.9998", service.stateFlow.value.balance)
        assertBigDecimalEquals("3.9999", service.stateFlow.value.displayBalance)
    }

    @Test
    fun feeChanged_zcashNativeWithLocalPending_updatesAvailableBalance() = runTest {
        stubZcashBalance()

        val service = TokenBalanceService(adapterManager, marketKit)
        val serviceScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        try {
            service.start(serviceScope)
            service.setToken(token)

            feeFlow.value = BigDecimal("0.0002")
            advanceUntilIdle()

            assertBigDecimalEquals("3.9997", service.stateFlow.value.balance)
            assertBigDecimalEquals("3.9999", service.stateFlow.value.displayBalance)
        } finally {
            serviceScope.cancel()
        }
    }

    @Test
    fun balanceUpdated_zcashNativeWithLocalPending_updatesAvailableBalance() = runTest {
        stubZcashBalance()

        val service = TokenBalanceService(adapterManager, marketKit)
        val serviceScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        try {
            service.start(serviceScope)
            service.setToken(token)

            adjustedBalanceData = BalanceData(available = BigDecimal("4.9999"))
            balanceUpdatedFlow.emit(Unit)
            advanceUntilIdle()

            assertBigDecimalEquals("4.9998", service.stateFlow.value.balance)
            assertBigDecimalEquals("4.9999", service.stateFlow.value.displayBalance)
        } finally {
            serviceScope.cancel()
        }
    }

    private fun stubZcashBalance() {
        every { adapterManager.getAdapterForToken<IBalanceAdapter>(token) } returns adapter
        every { adapterManager.getAdjustedBalanceDataForToken(token) } answers { adjustedBalanceData }
        every { adapter.balanceData } answers { adapterBalanceData }
        every { adapter.balanceUpdatedFlow } returns balanceUpdatedFlow
        every { adapter.fee } returns feeFlow
        every { marketKit.token(any()) } returns token
    }

    private fun assertBigDecimalEquals(expected: String, actual: BigDecimal?) {
        assertEquals(BigDecimal(expected).stripTrailingZeros(), actual?.stripTrailingZeros())
    }
}
