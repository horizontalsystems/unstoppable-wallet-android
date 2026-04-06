package cash.p.terminal.core

import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.BalanceData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class AdapterManagerFeeBalanceTest {

    private val feeToken = mockk<Token>(relaxed = true)
    private val currentToken = mockk<Token>(relaxed = true)

    @Test
    fun getFeeTokenBalance_adjustedBalanceExists_returnsAdjustedBalance() {
        val adapterManager = mockk<IAdapterManager>(relaxed = true)
        val adjustedBalance = BigDecimal("2.4588")

        every { adapterManager.getAdjustedBalanceDataForToken(feeToken) } returns BalanceData(adjustedBalance)

        val result = adapterManager.getFeeTokenBalance(feeToken, currentToken)

        assertEquals(adjustedBalance, result)
    }

    @Test
    fun getFeeTokenBalance_adjustedBalanceMissing_returnsNativeBalanceFromCurrentTokenAdapter() {
        val adapterManager = mockk<IAdapterManager>(relaxed = true)
        val nativeBalance = BigDecimal("2.4588")
        val adapter = TestNativeBalanceAdapter(nativeBalance)

        every { adapterManager.getAdjustedBalanceDataForToken(feeToken) } returns null
        every { adapterManager.getAdapterForToken<IBalanceAdapter>(currentToken) } returns adapter

        val result = adapterManager.getFeeTokenBalance(feeToken, currentToken)

        assertEquals(nativeBalance, result)
    }

    @Test
    fun getFeeTokenBalance_noBalanceSources_returnsNull() {
        val adapterManager = mockk<IAdapterManager>(relaxed = true)

        every { adapterManager.getAdjustedBalanceDataForToken(feeToken) } returns null
        every { adapterManager.getAdapterForToken<IBalanceAdapter>(currentToken) } returns mockk(relaxed = true)

        val result = adapterManager.getFeeTokenBalance(feeToken, currentToken)

        assertNull(result)
    }

    private class TestNativeBalanceAdapter(
        nativeAvailable: BigDecimal,
    ) : IBalanceAdapter, INativeBalanceProvider {
        override val balanceState: AdapterState = AdapterState.Synced
        override val balanceStateUpdatedFlow: Flow<Unit> = emptyFlow()
        override val balanceData: BalanceData = BalanceData(BigDecimal.ZERO)
        override val balanceUpdatedFlow: Flow<Unit> = emptyFlow()
        override val fee = MutableStateFlow(BigDecimal.ZERO)
        override val nativeBalanceData: BalanceData = BalanceData(nativeAvailable)
        override val nativeBalanceUpdatedFlow: Flow<Unit> = emptyFlow()
    }
}
