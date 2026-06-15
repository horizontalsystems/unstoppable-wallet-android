package cash.p.terminal.modules.send.zcash

import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ZcashSendBalanceTest {

    @Test
    fun calculateZcashAvailableToSend_noLocalPending_subtractsFeeFromAdapterAvailable() {
        val availableToSend = calculateZcashAvailableToSend(
            adjustedAvailable = null,
            adapterAvailable = BigDecimal("4.065"),
            fee = BigDecimal("0.0001")
        )

        assertBigDecimalEquals("4.0649", availableToSend)
    }

    @Test
    fun calculateZcashAvailableToSend_localPending_subtractsFeeFromAdjustedAvailable() {
        val availableToSend = calculateZcashAvailableToSend(
            adjustedAvailable = BigDecimal("3.9999"),
            adapterAvailable = BigDecimal("4.9999"),
            fee = BigDecimal("0.0001")
        )

        assertBigDecimalEquals("3.9998", availableToSend)
    }

    @Test
    fun calculateZcashAvailableToSend_feeGreaterThanAvailable_returnsZero() {
        val availableToSend = calculateZcashAvailableToSend(
            adjustedAvailable = BigDecimal("0.00005"),
            adapterAvailable = BigDecimal("0.5"),
            fee = BigDecimal("0.0001")
        )

        assertBigDecimalEquals("0", availableToSend)
    }

    @Test
    fun getZcashSdkBalance_adapterAvailable_returnsAdapterAvailable() {
        val wallet = mockk<Wallet>()
        val balanceAdapter = mockk<IBalanceAdapter> {
            every { balanceData } returns BalanceData(available = BigDecimal("4.065"))
        }
        val adapterManager = mockk<IAdapterManager> {
            every { getBalanceAdapterForWallet(wallet) } returns balanceAdapter
        }

        val sdkBalance = adapterManager.getZcashSdkBalance(
            wallet = wallet,
            fallback = BigDecimal("3")
        )

        assertBigDecimalEquals("4.065", sdkBalance)
    }

    @Test
    fun getZcashSdkBalance_missingAdapter_returnsFallback() {
        val wallet = mockk<Wallet>()
        val adapterManager = mockk<IAdapterManager> {
            every { getBalanceAdapterForWallet(wallet) } returns null
        }

        val sdkBalance = adapterManager.getZcashSdkBalance(
            wallet = wallet,
            fallback = BigDecimal("3")
        )

        assertBigDecimalEquals("3", sdkBalance)
    }

    private fun assertBigDecimalEquals(expected: String, actual: BigDecimal) {
        assertEquals(BigDecimal(expected).stripTrailingZeros(), actual.stripTrailingZeros())
    }
}
