package cash.p.terminal.core.adapters.zcash

import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ZcashBalanceDataTest {

    @Test
    fun toBalanceData_pendingBalance_mapsToProcessingBalance() {
        val balanceData = WalletBalance(
            available = Zatoshi(100_000_000),
            changePending = Zatoshi(20_000_000),
            valuePending = Zatoshi(30_000_000)
        ).toBalanceData(8)

        assertBigDecimalEquals("1", balanceData.available)
        assertBigDecimalEquals("0.5", balanceData.pending)
        assertBigDecimalEquals("1.5", balanceData.total)
    }

    @Test
    fun toBalanceData_availableBalance_excludesPending() {
        val balanceData = WalletBalance(
            available = Zatoshi(0),
            changePending = Zatoshi(406_500_000),
            valuePending = Zatoshi(0)
        ).toBalanceData(8)

        assertBigDecimalEquals("0", balanceData.available)
        assertBigDecimalEquals("4.065", balanceData.pending)
        assertBigDecimalEquals("4.065", balanceData.total)
    }

    private fun assertBigDecimalEquals(expected: String, actual: BigDecimal) {
        assertEquals(BigDecimal(expected).stripTrailingZeros(), actual.stripTrailingZeros())
    }
}
