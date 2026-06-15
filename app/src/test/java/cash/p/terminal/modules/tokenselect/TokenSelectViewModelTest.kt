package cash.p.terminal.modules.tokenselect

import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.balance.BalanceItem
import cash.p.terminal.wallet.entities.BalanceData
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TokenSelectViewModelTest {

    @Test
    fun hasSelectableBalance_pendingOnlyBalance_returnsTrue() {
        val item = balanceItem(
            BalanceData(
                available = BigDecimal.ZERO,
                pending = BigDecimal.ONE
            )
        )

        assertTrue(item.hasSelectableBalance())
    }

    @Test
    fun hasSelectableBalance_zeroTotalBalance_returnsFalse() {
        val item = balanceItem(BalanceData(available = BigDecimal.ZERO))

        assertFalse(item.hasSelectableBalance())
    }

    private fun balanceItem(balanceData: BalanceData) = BalanceItem(
        wallet = mockk<Wallet>(relaxed = true),
        balanceData = balanceData,
        state = AdapterState.Synced,
        sendAllowed = true,
        coinPrice = null,
    )
}
